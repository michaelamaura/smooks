/*-
 * ========================LICENSE_START=================================
 * Core
 * %%
 * Copyright (C) 2020 Smooks
 * %%
 * Licensed under the terms of the Apache License Version 2.0, or
 * the GNU Lesser General Public License version 3.0 or later.
 *
 * SPDX-License-Identifier: Apache-2.0 OR LGPL-3.0-or-later
 *
 * ======================================================================
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ======================================================================
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 * =========================LICENSE_END==================================
 */
package org.smooks.engine.lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smooks.api.lifecycle.LifecyclePhase;
import org.smooks.api.SmooksConfigException;
import org.smooks.support.classpath.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class AbstractLifecyclePhase implements LifecyclePhase {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractLifecyclePhase.class);

    @Override
    public void apply(Object o) {
        doApply(o);
    }

    public void doApply(Object o) {

    }

    protected <U> void invoke(U instance, Class<? extends Annotation> annotation) {
        Method[] methods = instance.getClass().getMethods();

        for (Method method : methods) {
            if (method.getAnnotation(annotation) != null) {
                if (method.getParameterTypes().length == 0) {
                    Class<?>[] exceptionTypes = method.getExceptionTypes();
                    if (method.getExceptionTypes().length > 0) {
                        Optional<Class<?>> checkedExceptionTypes = Arrays.stream(exceptionTypes).filter(t -> !(RuntimeException.class.isAssignableFrom(t) || Error.class.isAssignableFrom(t))).findFirst();
                        if (checkedExceptionTypes.isPresent()) {
                            String[] exceptionTypesClassNames = Arrays.stream(exceptionTypes).map(Class::getName).collect(Collectors.toList()).toArray(new String[]{});
                            LOGGER.warn("The lifecycle method [{}] should not throw a checked exception. Future versions of Smooks may fail during initialisation if a checked exception is thrown from a [@{}] method. Related annotation information: annotation [@{}()] on annotated element [{}() throws {}] of type [METHOD]", method.getName(), annotation.getSimpleName(), annotation.getName(), instance.getClass().getName() + "." + method.getName(), String.join(",", exceptionTypesClassNames));
                        }
                    }
                    try {
                        method.invoke(instance);
                    } catch (IllegalAccessException e) {
                        throw new SmooksConfigException("Error invoking [@" + annotation.getSimpleName() + "] method [" + method.getName() + "] on class [" + instance.getClass().getName() + "]", e);
                    } catch (InvocationTargetException e) {
                        throw new SmooksConfigException("Error invoking [@" + annotation.getSimpleName() + "] method [" + method.getName() + "] on class [" + instance.getClass().getName() + "]", e.getTargetException());
                    }
                } else {
                    LOGGER.warn("Method [{}] defines an @{} annotation on a parameterised method. This is not allowed!", ClassUtils.getLongMemberName(method), annotation.getSimpleName());
                }
            }
        }
    }
}

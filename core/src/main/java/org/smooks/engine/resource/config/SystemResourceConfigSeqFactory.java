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
package org.smooks.engine.resource.config;

import org.smooks.api.SmooksException;
import org.smooks.api.resource.ContainerResourceLocator;
import org.smooks.api.resource.config.ResourceConfigSeq;
import org.smooks.api.resource.config.ResourceConfigSeqFactory;
import org.smooks.api.resource.config.loader.ResourceConfigLoader;
import org.smooks.engine.resource.config.loader.xml.XmlResourceConfigLoader;

import java.io.IOException;
import java.io.InputStream;

public class SystemResourceConfigSeqFactory implements ResourceConfigSeqFactory {

    private final ClassLoader classLoader;
    private final String resourceFile;
    private final ContainerResourceLocator resourceLocator;
    private final ResourceConfigLoader resourceConfigLoader;

    public SystemResourceConfigSeqFactory(String resourceFile, ClassLoader classLoader, ContainerResourceLocator resourceLocator, ResourceConfigLoader resourceConfigLoader) {
        this.classLoader = classLoader;
        this.resourceFile = resourceFile;
        this.resourceLocator = resourceLocator;
        this.resourceConfigLoader = resourceConfigLoader;
    }

    @Override
    public ResourceConfigSeq create() {
        InputStream resource;
        try {
            resource = resourceLocator.getResource(resourceFile);
        } catch (IOException e) {
            throw new SmooksException(e);
        }

        if (resource == null) {
            throw new IllegalStateException(String.format("Failed to load [%s]", resourceFile));
        }
        try {
            ResourceConfigSeq resourceConfigSeq = resourceConfigLoader.load(resource, resourceFile, classLoader);
            for (int i = 0; i < resourceConfigSeq.size(); i++) {
                resourceConfigSeq.get(i).setSystem(true);
            }
            resourceConfigSeq.setSystem(true);

            return resourceConfigSeq;
        } catch (Exception e) {
            throw new SmooksException("Error processing resource file '" + resourceFile + "'.", e);
        }
    }
}

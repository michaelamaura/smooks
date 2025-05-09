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
package org.smooks.engine.delivery;

import org.smooks.api.resource.config.ResourceConfig;
import org.smooks.api.delivery.ResourceConfigExpander;
import org.smooks.api.SmooksConfigException;
import org.smooks.engine.resource.config.DefaultResourceConfig;
import org.smooks.api.ExecutionContext;
import org.smooks.api.resource.visitor.dom.DOMElementVisitor;
import org.smooks.engine.resource.visitor.dom.DefaultDOMSerializerVisitor;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author
 */
public class TestExpandableContentHandler implements DOMElementVisitor, ResourceConfigExpander {

    public void setConfiguration(ResourceConfig resourceConfig) throws SmooksConfigException {
    }

    @Override
    public List<ResourceConfig> expandConfigurations() {

        List<ResourceConfig> expansionConfigs = new ArrayList<ResourceConfig>();

        expansionConfigs.add(new DefaultResourceConfig("a", new Properties(), Assembly1.class.getName()));
        expansionConfigs.add(new DefaultResourceConfig("b", new Properties(), Processing1.class.getName()));
        expansionConfigs.add(new DefaultResourceConfig("c", new Properties(), DefaultDOMSerializerVisitor.class.getName()));

        return expansionConfigs;
    }

    @Override
    public void visitBefore(Element element, ExecutionContext executionContext) {
    }

    @Override
    public void visitAfter(Element element, ExecutionContext executionContext) {
    }
}

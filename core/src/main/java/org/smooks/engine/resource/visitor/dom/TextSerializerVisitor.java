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
package org.smooks.engine.resource.visitor.dom;

import org.smooks.api.ExecutionContext;
import org.smooks.api.SmooksException;
import org.smooks.api.resource.visitor.sax.ng.BeforeVisitor;
import org.smooks.engine.xml.Namespace;
import org.smooks.support.DomUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.Writer;

/**
 * Write a &lt;text&gt; element.
 * <p/>
 * Basically just drops the &lt;text&gt; tags.
 *
 * @author <a href="mailto:tom.fennelly@jboss.com">tom.fennelly@jboss.com</a>
 */
public class TextSerializerVisitor extends DefaultDOMSerializerVisitor implements BeforeVisitor {

    @Override
    public void writeStartElement(Element element, Writer writer, ExecutionContext executionContext) throws IOException {
    }

    @Override
    public void writeEndElement(Element element, Writer writer, ExecutionContext executionContext) throws IOException {
    }

    public static Element createTextElement(Element element, String templatingResult) {
        Document ownerDocument = element.getOwnerDocument();
        Element resultElement = ownerDocument.createElementNS(Namespace.SMOOKS_URI, "text");
        resultElement.appendChild(ownerDocument.createTextNode(templatingResult));
        return resultElement;
    }

    public static boolean isTextElement(Element element) {
        return DomUtils.getName(element).equals("text") && Namespace.SMOOKS_URI.equals(element.getNamespaceURI());
    }

    public static String getText(Element element) {
        return DomUtils.getAllText(element, false);
    }

    @Override
    public void visitBefore(Element element, ExecutionContext executionContext) throws SmooksException {

    }
}

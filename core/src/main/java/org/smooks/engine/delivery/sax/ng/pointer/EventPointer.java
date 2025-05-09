/*-
 * ========================LICENSE_START=================================
 * Core
 * %%
 * Copyright (C) 2020 - 2021 Smooks
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
package org.smooks.engine.delivery.sax.ng.pointer;

import org.smooks.api.SmooksException;
import org.smooks.api.ExecutionContext;
import org.smooks.api.TypedKey;
import org.smooks.engine.delivery.event.VisitSequence;
import org.smooks.engine.delivery.interceptor.EventPointerStaticProxyInterceptor;
import org.smooks.engine.xml.Namespace;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Represents a <i>pointer</i> to an event and provides convenience methods to retrieve attribute values from the event.
 * <br/><br/>
 * A <i>pointer</i> captures the state of an execution in a point of time. Formally, a <i>pointer</i> is a pair of attributes:
 * <li>
 * <il>reference: key to an execution context value holding the event</il>
 * <il>visit: name of the visit method that the event is targeting</il>
 * </li>
 * <p>
 * Pointer nodes are meant for nested Smooks executions (i.e., a Smooks execution within another Smooks execution).
 * It allows the outer execution to carry over its visit state to the nested execution with the help of
 * {@link EventPointerStaticProxyInterceptor}. Without a <i>pointer</i>, the nested Smooks instance
 * cannot join the inner execution to the outer one.
 */
public class EventPointer {

    private final Node pointerNode;

    public EventPointer(Node node) {
        if (!isPointer(node)) {
            throw new SmooksException("Node is not a pointer element");
        }
        this.pointerNode = node;
    }

    public EventPointer(Document factory, VisitSequence visitSequence) {
        final Element pointerElement = factory.createElementNS(Namespace.SMOOKS_URI, "pointer");
        pointerElement.setAttribute("visit", visitSequence.toString());
        pointerElement.setAttribute("reference", TypedKey.of().getName());

        this.pointerNode = pointerElement;
    }

    public Node getPointerNode() {
        return pointerNode;
    }

    /**
     * Checks whether a node is a <i>pointer</i> element.
     *
     * @param node the node to be tested
     * @return <code>true</code> if the node is a pointer otherwise <code>false</code>
     */
    public static boolean isPointer(Node node) {
        return node instanceof Element && node.getNamespaceURI() != null &&
                node.getNamespaceURI().equals(Namespace.SMOOKS_URI) &&
                node.getLocalName().equals("pointer");
    }

    /**
     * Gets the execution context key which maps to the node representing the event.
     *
     * @return the key of the execution context entry that holds the event node
     */
    public TypedKey<Node> getReference() {
        return TypedKey.of(pointerNode.getAttributes().getNamedItem("reference").getNodeValue());
    }

    /**
     * Gets the name of the visit this <code>EventPointer</code> is targeting.
     *
     * @return the name of the visit
     */
    public VisitSequence getVisit() {
        return VisitSequence.valueOf(pointerNode.getAttributes().getNamedItem("visit").getNodeValue());
    }

    /**
     * Provides a convenience method to retrieve the source node.
     *
     * @param executionContext the execution context holding the source
     * @return the source node or <code>null</code> if not found
     */
    public Node dereference(ExecutionContext executionContext) {
        return executionContext.get(getReference());
    }

}
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

import org.junit.jupiter.api.Test;
import org.smooks.Smooks;
import org.smooks.StreamFilterType;
import org.smooks.api.ExecutionContext;
import org.smooks.api.SmooksException;
import org.smooks.api.delivery.Filter;
import org.smooks.api.resource.visitor.Visitor;
import org.smooks.api.resource.visitor.interceptor.InterceptorVisitor;
import org.smooks.engine.delivery.dom.DOMContentDeliveryConfig;
import org.smooks.engine.delivery.sax.ng.SaxNgContentDeliveryConfig;
import org.smooks.engine.delivery.sax.ng.Visitor01;
import org.smooks.testkit.TextUtils;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class DefaultContentDeliveryConfigBuilderTestCase {

    @Test
    public void testSax() throws IOException, SAXException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("smooks-config-sax.xml"));
        ExecutionContext execContext = smooks.createExecutionContext();

        assertInstanceOf(SaxNgContentDeliveryConfig.class, execContext.getContentDeliveryRuntime().getContentDeliveryConfig());
        SaxNgContentDeliveryConfig config = (SaxNgContentDeliveryConfig) execContext.getContentDeliveryRuntime().getContentDeliveryConfig();

        assertEquals(8, config.getBeforeVisitorIndex().size());
        assertInstanceOf(Visitor01.class, ((InterceptorVisitor) config.getBeforeVisitorIndex().get("b").get(0).getContentHandler()).getTarget().getContentHandler());
        assertEquals(8, config.getAfterVisitorIndex().size());
        assertInstanceOf(Visitor01.class, ((InterceptorVisitor) config.getAfterVisitorIndex().get("b").get(0).getContentHandler()).getTarget().getContentHandler());
    }

    @Test
    public void testDom() throws IOException, SAXException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("smooks-config-dom.xml"));
        ExecutionContext execContext = smooks.createExecutionContext();

        assertInstanceOf(DOMContentDeliveryConfig.class, execContext.getContentDeliveryRuntime().getContentDeliveryConfig());
        DOMContentDeliveryConfig config = (DOMContentDeliveryConfig) execContext.getContentDeliveryRuntime().getContentDeliveryConfig();

        assertEquals(1, config.getAssemblyVisitBeforeIndex().values().stream().mapToLong(Collection::size).sum());
        assertEquals(1, config.getAssemblyVisitAfterIndex().values().stream().mapToLong(Collection::size).sum());
        assertEquals(3, config.getProcessingVisitBeforeIndex().values().stream().mapToLong(Collection::size).sum());
        assertEquals(3, config.getProcessingVisitAfterIndex().values().stream().mapToLong(Collection::size).sum());
        assertEquals(4, config.getSerializerVisitorIndex().values().stream().mapToLong(Collection::size).sum());
    }

    @Test
    public void testDomSax1() throws IOException, SAXException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("smooks-config-dom-sax-1.xml"));
        ExecutionContext execContext = smooks.createExecutionContext();

        // Should default to SAX
        assertInstanceOf(SaxNgContentDeliveryConfig.class, execContext.getContentDeliveryRuntime().getContentDeliveryConfig());
    }

    @Test
    public void testDomSax2() throws IOException, SAXException {
        Smooks smooks;
        ExecutionContext execContext;

        smooks = new Smooks(getClass().getResourceAsStream("smooks-config-dom-sax-2.1.xml"));
        execContext = smooks.createExecutionContext();
        assertInstanceOf(SaxNgContentDeliveryConfig.class, execContext.getContentDeliveryRuntime().getContentDeliveryConfig());

        smooks = new Smooks(getClass().getResourceAsStream("smooks-config-dom-sax-2.2.xml"));
        execContext = smooks.createExecutionContext();
        assertInstanceOf(DOMContentDeliveryConfig.class, execContext.getContentDeliveryRuntime().getContentDeliveryConfig());

        smooks = new Smooks(getClass().getResourceAsStream("smooks-config-dom-sax-2.3.xml"));
        try {
            smooks.createExecutionContext();
            fail("Expected SmooksException");
        } catch (SmooksException e) {
            assertEquals("The configured filter [xxxx] cannot be used: [SAX NG, DOM] filters can be used for the given set of visitors. Turn on debug logging for more information", e.getMessage());
        }
    }

    @Test
    public void testDomSax3() throws IOException, SAXException {
        String origDefault = System.setProperty(Filter.STREAM_FILTER_TYPE, StreamFilterType.DOM.toString());

        try {
            Smooks smooks = new Smooks(getClass().getResourceAsStream("smooks-config-dom-sax-1.xml"));
            ExecutionContext execContext = smooks.createExecutionContext();

            // Should default to DOM
            assertInstanceOf(DOMContentDeliveryConfig.class, execContext.getContentDeliveryRuntime().getContentDeliveryConfig());
        } finally {
            if (origDefault != null) {
                System.setProperty(Filter.STREAM_FILTER_TYPE, origDefault);
            } else {
                System.getProperties().remove(Filter.STREAM_FILTER_TYPE);
            }
        }
    }

    @Test
    public void testUnsupportedVisitor() throws IOException, SAXException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("smooks-config-invalid.xml"));

        try {
            smooks.createExecutionContext();
            fail("Expected SmooksException");
        } catch (SmooksException e) {
            String expected = TextUtils.trimLines(getClass().getResourceAsStream("smooks-config-invalid-error.txt")).toString();
            String actual = TextUtils.trimLines(new StringReader(e.getMessage())).toString();

            assertEquals(expected.toLowerCase(), actual.toLowerCase());
        }
    }

    public static class UnsupportedVisitor implements Visitor {

    }
}

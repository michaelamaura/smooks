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
package org.smooks.engine.delivery.lifecycle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import org.smooks.Smooks;
import org.smooks.io.source.StringSource;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class ExecutionLifecycleTestCase {

	@BeforeEach
    public void setUp() throws Exception {
        DomAssemblyBefore.cleaned = false;
        DomAssemblyAfter.cleaned = false;
        DomAssemblyAfterWithException.cleaned = false;
        DomProcessingBefore.initialized = false;
        DomProcessingBefore.cleaned = false;
        DomProcessingAfter.cleaned = false;
        SaxVisitBefore.initialized = false;
        SaxVisitBefore.cleaned = false;
        SaxVisitAfter.cleaned = false;
        DomProcessingPostFragmentLifecycle.cleaned = false;
        SaxVisitPostFragmentLifecycle.cleaned = false;
    }

	@Test
    public void test_dom_01() throws IOException, SAXException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("dom-config-01.xml"));

        smooks.filterSource(new StringSource("<a><b/><c/><d/><e/></a>"));
        assertTrue(DomAssemblyBefore.cleaned);
        assertTrue(DomAssemblyAfter.cleaned);
        assertTrue(DomAssemblyAfterWithException.cleaned);
        assertTrue(DomProcessingBefore.initialized);
        assertTrue(DomProcessingBefore.cleaned);
        assertTrue(DomProcessingAfter.cleaned);
    }

	@Test
    public void test_dom_02() throws IOException, SAXException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("dom-config-02.xml"));

        smooks.filterSource(new StringSource("<a></a>"));
        assertTrue(DomProcessingPostFragmentLifecycle.cleaned);
    }

	@Test
    public void test_SAX_01() throws IOException, SAXException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("sax-config-01.xml"));

        smooks.filterSource(new StringSource("<a><b/><c/><d/><e/></a>"));
        assertTrue(SaxVisitBefore.initialized);
        assertTrue(SaxVisitBefore.cleaned);
        assertTrue(SaxVisitAfter.cleaned);
    }

	@Test
    public void test_SAX_02() throws IOException, SAXException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("sax-config-02.xml"));

        smooks.filterSource(new StringSource("<a></a>"));
        assertTrue(SaxVisitPostFragmentLifecycle.cleaned);
    }
}

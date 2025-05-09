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
package org.smooks.engine.delivery.dom.serialize;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smooks.api.lifecycle.LifecycleManager;
import org.smooks.api.resource.config.ResourceConfig;
import org.smooks.engine.injector.Scope;
import org.smooks.engine.lifecycle.PostConstructLifecyclePhase;
import org.smooks.engine.lookup.LifecycleManagerLookup;
import org.smooks.engine.resource.config.DefaultResourceConfig;
import org.smooks.engine.resource.visitor.dom.Serializer;
import org.smooks.support.XmlUtils;
import org.smooks.testkit.MockExecutionContext;
import org.smooks.testkit.delivery.dom.MockContentDeliveryConfig;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.smooks.testkit.Assertions.compareCharStreams;

/**
 *
 * @author tfennelly
 */
@SuppressWarnings("unchecked")
public class SerializerTestCase {
	private static final Logger LOGGER = LoggerFactory.getLogger(SerializerTestCase.class);

	@Test
	public void testSerialize() throws IOException, SAXException {
		MockExecutionContext executionContext = new MockExecutionContext();
		LifecycleManager lifecycleManager = executionContext.getApplicationContext().getRegistry().lookup(new LifecycleManagerLookup());
		// Target a resource at the "document fragment" i.e. the root..

        // Don't write xxx but write its child elements
		ResourceConfig resourceConfig = new DefaultResourceConfig(ResourceConfig.DOCUMENT_FRAGMENT_SELECTOR, new Properties(), "deviceX", "....");
		AddAttributeSerializer addAttributeSerializer = new AddAttributeSerializer();
		lifecycleManager.applyPhase(addAttributeSerializer, new PostConstructLifecyclePhase(new Scope(executionContext.getApplicationContext().getRegistry(), resourceConfig, addAttributeSerializer)));
		((MockContentDeliveryConfig)executionContext.deliveryConfig).getSerializerVisitorIndex().put(ResourceConfig.DOCUMENT_FRAGMENT_SELECTOR, resourceConfig, addAttributeSerializer);

        // Don't write xxx but write its child elements
		resourceConfig = new DefaultResourceConfig("xxx", new Properties(), "deviceX", "....");
		RemoveTestSerializaterVisitor removeTestSerializationUnit = new RemoveTestSerializaterVisitor();
		lifecycleManager.applyPhase(removeTestSerializationUnit, new PostConstructLifecyclePhase(new Scope(executionContext.getApplicationContext().getRegistry(), resourceConfig, removeTestSerializationUnit)));

		((MockContentDeliveryConfig)executionContext.deliveryConfig).getSerializerVisitorIndex().put("xxx", resourceConfig, removeTestSerializationUnit);

		// write yyyy as a badly-formed empty element
		resourceConfig = new DefaultResourceConfig("yyyy", new Properties(), "deviceX", "....");
		resourceConfig.setParameter("wellformed", "false");
		EmptyElTestSerializerVisitor emptyElTestSerializationUnit = new EmptyElTestSerializerVisitor();
		lifecycleManager.applyPhase(emptyElTestSerializationUnit, new PostConstructLifecyclePhase(new Scope(executionContext.getApplicationContext().getRegistry(), resourceConfig, emptyElTestSerializationUnit)));
		
		((MockContentDeliveryConfig)executionContext.deliveryConfig).getSerializerVisitorIndex().put("yyyy", resourceConfig, emptyElTestSerializationUnit);

		/// write zzz as a well-formed empty element
		resourceConfig = new DefaultResourceConfig("zzz", new Properties(), "deviceX", "....");
		EmptyElTestSerializerVisitor otherEmptyElTestSerializationUnit = new EmptyElTestSerializerVisitor();
		lifecycleManager.applyPhase(otherEmptyElTestSerializationUnit, new PostConstructLifecyclePhase(new Scope(executionContext.getApplicationContext().getRegistry(), resourceConfig, otherEmptyElTestSerializationUnit)));

		((MockContentDeliveryConfig) executionContext.deliveryConfig).getSerializerVisitorIndex().put("zzz", resourceConfig, otherEmptyElTestSerializationUnit);

		Document doc = XmlUtils.parseStream(getClass().getResourceAsStream("testmarkup.xxml"), XmlUtils.VALIDATION_TYPE.NONE, true);
		Serializer serializer = new Serializer(doc, executionContext);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		OutputStreamWriter writer = new OutputStreamWriter(output);

		serializer.serialize(writer);
		writer.flush();
		byte[] actualBytes = output.toByteArray();
		LOGGER.debug(new String(actualBytes));
		boolean areEqual = compareCharStreams(getClass().getResourceAsStream("testmarkup.xxml.ser_1"), new ByteArrayInputStream(actualBytes));
		assertTrue(areEqual, "Unexpected Serialization result failure.");
	}
}

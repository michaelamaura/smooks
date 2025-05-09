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
package org.smooks.io;

import org.junit.jupiter.api.Test;
import org.smooks.api.SmooksException;
import org.smooks.api.ExecutionContext;
import org.smooks.testkit.MockExecutionContext;
import org.smooks.api.TypedKey;
import org.smooks.engine.delivery.fragment.NodeFragment;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for AbstractOutputStreamResouce 
 * 
 * @author <a href="mailto:daniel.bevenius@gmail.com">Daniel Bevenius</a>
 */
public class AbstractOutputStreamResourceTestCase {
    @Test
    public void getOutputStream() throws ParserConfigurationException {
        AbstractOutputStreamResource resource = new MockAbstractOutputStreamResource();
        MockExecutionContext executionContext = new MockExecutionContext();

        assertNull(getResource(resource, executionContext));
        resource.visitBefore(null, executionContext);
        assertNotNull(getResource(resource, executionContext));

        ResourceOutputStream outputStreamWriter = new ResourceOutputStream(executionContext, resource.getResourceName());
        OutputStream outputStream = outputStreamWriter.getDelegateOutputStream();
        assertNotNull(outputStream);
        assertTrue(outputStream instanceof ByteArrayOutputStream);

        // Should get an error now if we try get a writer to the same resource...
        try {
            new ResourceWriter(executionContext, resource.getResourceName());
            fail("Expected SmooksException");
        } catch (SmooksException e) {
            assertEquals("An output stream to the [Mock] resource is already open. Cannot open a Writer to this resource now", e.getMessage());
        }

        resource.onPostFragment(new NodeFragment(DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()), executionContext);

        // Should be unbound "after" and the stream should be closed...
        assertNull(getResource(resource, executionContext));
        assertTrue(MockAbstractOutputStreamResource.isClosed);
    }

    @Test
    public void getOutputWriter() throws ParserConfigurationException {
        AbstractOutputStreamResource resource = new MockAbstractOutputStreamResource();
        MockExecutionContext executionContext = new MockExecutionContext();

        assertNull(getResource(resource, executionContext));
        resource.visitBefore(null, executionContext);
        assertNotNull(getResource(resource, executionContext));

        Writer writer = new ResourceWriter(executionContext, resource.getResourceName()).getDelegateWriter();
        assertNotNull(writer);
        assertTrue(writer instanceof java.io.OutputStreamWriter);

        // Should get an error now if we try get an OutputStream to the same resource...
        try {
            new ResourceOutputStream(executionContext, resource.getResourceName());
            fail("Expected SmooksException");
        } catch (SmooksException e) {
            assertEquals("An Writer to the 'Mock' resource is already open.  Cannot open an OutputStream to this resource now!", e.getMessage());
        }

        resource.onPostFragment(new NodeFragment(DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()), executionContext);

        // Should be unbound "after" and the stream should be closed...
        assertNull(getResource(resource, executionContext));
        assertTrue(MockAbstractOutputStreamResource.isClosed);
    }

    private Object getResource(AbstractOutputStreamResource resource, MockExecutionContext executionContext) {
        return executionContext.get(TypedKey.of(AbstractOutputStreamResource.RESOURCE_CONTEXT_KEY_PREFIX + resource.getResourceName()));
    }

    /**
     * Mock class for testing
     */
    private static class MockAbstractOutputStreamResource extends AbstractOutputStreamResource {
        public static boolean isClosed;

        @Override
        public OutputStream getOutputStream(final ExecutionContext executionContext) {
            isClosed = false;
            return new ByteArrayOutputStream() {
                @Override
                public void close() throws IOException {
                    isClosed = true;
                    super.close();
                }
            };
        }

        @Override
        public String getResourceName() {
            return "Mock";
        }

        @Override
        public Charset getWriterEncoding() {
            return StandardCharsets.UTF_8;
        }
    }
}

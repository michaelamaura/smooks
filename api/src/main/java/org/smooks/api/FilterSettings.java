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
package org.smooks.api;

import org.smooks.api.delivery.StreamFilterType;

/**
 * Filter settings for programmatic configuration of a {@link org.smooks.Smooks} instance.
 * {@link org.smooks.engine.DefaultFilterSettings} is the main implementation of this interface. Filter settings are applied
 * programmatically to a {@link org.smooks.Smooks} instance from the {@link org.smooks.api.ApplicationContextBuilder}.
 */
public interface FilterSettings {

    /**
     *
     * @param streamFilterType
     * @return
     */
    FilterSettings setFilterType(StreamFilterType streamFilterType);

    /**
     *
     * @param rewriteEntities
     * @return
     */
    FilterSettings setRewriteEntities(boolean rewriteEntities);

    /**
     *
     * @param defaultSerializationOn
     * @return
     */
    FilterSettings setDefaultSerializationOn(boolean defaultSerializationOn);

    /**
     *
     * @param terminateOnException
     * @return
     */
    FilterSettings setTerminateOnException(boolean terminateOnException);

    /**
     *
     * @param maintainElementStack
     * @return
     */
    FilterSettings setMaintainElementStack(boolean maintainElementStack);

    /**
     *
     * @param closeSource
     * @return
     */
    FilterSettings setCloseSource(boolean closeSource);

    /**
     *
     * @param closeSink
     * @return
     */
    FilterSettings setCloseSink(boolean closeSink);

    /**
     *
     * @param readerPoolSize
     * @return
     */
    FilterSettings setReaderPoolSize(int readerPoolSize);

    /**
     *
     * @param maxNodeDepth
     * @return
     */
    FilterSettings setMaxNodeDepth(final int maxNodeDepth);

    StreamFilterType getFilterType();

    Boolean isRewriteEntities();

    Boolean isDefaultSerializationOn();

    Boolean isTerminateOnException();

    Boolean isMaintainElementStack();

    Boolean isCloseSource();

    Boolean isCloseSink();

    Integer getReaderPoolSize();

    Integer getMaxNodeDepth();
}


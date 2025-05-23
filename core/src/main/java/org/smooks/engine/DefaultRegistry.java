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
package org.smooks.engine;

import com.fasterxml.classmate.TypeResolver;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smooks.api.NotAppContextScoped;
import org.smooks.api.Registry;
import org.smooks.api.SmooksException;
import org.smooks.api.TypedKey;
import org.smooks.api.converter.TypeConverterFactory;
import org.smooks.api.lifecycle.LifecycleManager;
import org.smooks.api.profile.ProfileSet;
import org.smooks.api.profile.ProfileStore;
import org.smooks.api.resource.config.ResourceConfig;
import org.smooks.api.resource.config.ResourceConfigSeq;
import org.smooks.api.resource.config.loader.ResourceConfigLoader;
import org.smooks.assertion.AssertArgument;
import org.smooks.engine.converter.TypeConverterFactoryLoader;
import org.smooks.engine.injector.Scope;
import org.smooks.engine.lifecycle.DefaultLifecycleManager;
import org.smooks.engine.lifecycle.PostConstructLifecyclePhase;
import org.smooks.engine.lifecycle.PreDestroyLifecyclePhase;
import org.smooks.engine.lookup.LifecycleManagerLookup;
import org.smooks.engine.lookup.GlobalResourceConfigSeqLookup;
import org.smooks.engine.lookup.ResourceConfigSeqsLookup;
import org.smooks.engine.lookup.converter.TypeConverterFactoryLookup;
import org.smooks.engine.resource.config.DefaultResourceConfigSeq;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class DefaultRegistry implements Registry {
    private static final Logger LOGGER = LoggerFactory.getLogger(Registry.class);

    private final Map<Object, Object> registry = new ConcurrentHashMap<>();
    private final ClassLoader classLoader;
    private final ResourceConfigLoader resourceConfigLoader;

    public DefaultRegistry(ClassLoader classLoader, ResourceConfigLoader resourceConfigLoader, ProfileStore profileStore) {
        AssertArgument.isNotNull(classLoader, "classLoader");
        AssertArgument.isNotNull(resourceConfigLoader, "resourceConfigLoader");
        AssertArgument.isNotNull(profileStore, "profileStore");

        this.classLoader = classLoader;
        registerObject(ProfileStore.class, profileStore);

        Set<TypeConverterFactory<?, ?>> typeConverterFactories = new TypeConverterFactoryLoader().load(classLoader);
        registerObject(TypeConverterFactoryLookup.TYPE_CONVERTER_FACTORY_REGISTRY_KEY, typeConverterFactories);
        registerObject(LifecycleManager.class, new DefaultLifecycleManager());

        // add the default list to the list.
        ResourceConfigSeq globalResourceConfigSeq = new DefaultResourceConfigSeq("global");
        globalResourceConfigSeq.setSystem(true);

        registerObject(ResourceConfigSeq.class, globalResourceConfigSeq);

        List<ResourceConfigSeq> resourceConfigSeqs = new ArrayList<>();
        resourceConfigSeqs.add(globalResourceConfigSeq);
        registerObject(new TypeResolver().resolve(List.class, ResourceConfigSeq.class), resourceConfigSeqs);

        this.resourceConfigLoader = resourceConfigLoader;
    }

    @Override
    public void registerObject(final Object value) {
        AssertArgument.isNotNull(value, "value");

        final String name;
        if (value.getClass().isAnnotationPresent(Resource.class) && !value.getClass().getAnnotation(Resource.class).name().isEmpty()) {
            String candidateName = value.getClass().getAnnotation(Resource.class).name();
            if (registry.containsKey(candidateName)) {
                name = candidateName + ":" + UUID.randomUUID();
            } else {
                name = candidateName;
            }
        } else {
            name = value.getClass().getName() + ":" + UUID.randomUUID();
        }
        registerObject(name, value);
    }

    @Override
    public void registerObject(final Object key, final Object value) {
        AssertArgument.isNotNull(key, "key");
        AssertArgument.isNotNull(value, "value");

        if (registry.putIfAbsent(key, value) != null) {
            throw new SmooksException(String.format("Duplicate registered object for object with key [%s]", key));
        }
    }

    @Override
    public void deRegisterObject(final Object key) {
        registry.remove(key);
    }

    @Override
    public <R> R lookup(final Function<Map<Object, Object>, R> function) {
        return function.apply(Collections.unmodifiableMap(registry));
    }

    @Override
    public <T> T lookup(final Object key) {
        T entry = (T) registry.get(key);
        if (entry instanceof NotAppContextScoped.Ref) {
            return (T) ((NotAppContextScoped.Ref) entry).get();
        } else {
            return entry;
        }
    }

    @Override
    public <T> T lookup(final TypedKey<T> key) {
        return lookup((Object) key);
    }

    /**
     * Register the set of resources specified in the supplied XML configuration
     * stream.
     *
     * @param baseURI     The base URI to be associated with the configuration stream.
     * @param inputStream XML resource configuration stream.
     * @return The ResourceConfigList created from the added resource configuration.
     * @see ResourceConfig
     */
    @Override
    public ResourceConfigSeq registerResources(final String baseURI, final InputStream inputStream) {
        AssertArgument.isNotEmpty(baseURI, "baseURI");
        AssertArgument.isNotNull(inputStream, "inputStream");

        ResourceConfigSeq resourceConfigSeq = resourceConfigLoader.load(inputStream, baseURI, classLoader);
        registerResourceConfigSeq(resourceConfigSeq);

        return resourceConfigSeq;
    }

    protected void addProfileSets(final List<ProfileSet> profileSets) {
        final ProfileStore profileStore = lookup(ProfileStore.class);
        if (profileSets == null) {
            return;
        }

        for (ProfileSet profileSet : profileSets) {
            profileStore.addProfileSet(profileSet);
        }
    }


    /**
     * Register a {@link ResourceConfig} on this context store.
     * <p/>
     * The config gets added to the default resource list.
     *
     * @param resourceConfig The Content Delivery Resource definition to be registered.
     */
    @Override
    public void registerResourceConfig(final ResourceConfig resourceConfig) {
        AssertArgument.isNotNull(resourceConfig, "resourceConfig");

        lookup(new LifecycleManagerLookup()).applyPhase(resourceConfig, new PostConstructLifecyclePhase(new Scope(this)));
        lookup(new GlobalResourceConfigSeqLookup()).add(resourceConfig);
    }

    /**
     * Add a {@link ResourceConfigSeq} to this registry.
     *
     * @param resourceConfigSeq All the ResourceConfigList instances added on this registry.
     */
    @Override
    public void registerResourceConfigSeq(final ResourceConfigSeq resourceConfigSeq) {
        lookup(new ResourceConfigSeqsLookup()).add(resourceConfigSeq);
        lookup(new LifecycleManagerLookup()).applyPhase(resourceConfigSeq, new PostConstructLifecyclePhase(new Scope(this)));

        // XSD v1.0 added profiles to the resource config.  If there were any, add them to the
        // profile store.
        addProfileSets(resourceConfigSeq.getProfiles());
    }

    @Override
    public void close() {
        LOGGER.debug("Un-initializing all ContentHandler instances allocated through this registry");
        for (Object registeredObject : registry.values()) {
            LOGGER.debug("Un-initializing ContentHandler instance: {}", registeredObject.getClass().getName());
            try {
                lookup(new LifecycleManagerLookup()).applyPhase(registeredObject, new PreDestroyLifecyclePhase());
            } catch (Throwable throwable) {
                LOGGER.error("Error un-initializing " + registeredObject.getClass().getName() + ".", throwable);
            }
        }
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }
}

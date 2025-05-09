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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smooks.api.Registry;
import org.smooks.api.SmooksConfigException;
import org.smooks.api.delivery.ContentDeliveryConfig;
import org.smooks.api.delivery.ContentDeliveryConfigBuilder;
import org.smooks.api.delivery.ContentHandler;
import org.smooks.api.delivery.ContentHandlerBinding;
import org.smooks.api.delivery.ContentHandlerFactory;
import org.smooks.api.delivery.Filter;
import org.smooks.api.delivery.FilterProvider;
import org.smooks.api.delivery.ResourceConfigExpander;
import org.smooks.api.delivery.VisitorAppender;
import org.smooks.api.delivery.event.ContentDeliveryConfigExecutionEvent;
import org.smooks.api.lifecycle.ContentDeliveryConfigLifecycle;
import org.smooks.api.lifecycle.LifecycleManager;
import org.smooks.api.lifecycle.LifecyclePhase;
import org.smooks.api.profile.ProfileSet;
import org.smooks.api.resource.config.ResourceConfig;
import org.smooks.api.resource.config.ResourceConfigSortComparator;
import org.smooks.api.resource.config.xpath.SelectorStep;
import org.smooks.api.resource.visitor.Visitor;
import org.smooks.assertion.AssertArgument;
import org.smooks.engine.delivery.event.DefaultContentDeliveryConfigExecutionEvent;
import org.smooks.engine.lifecycle.ContentDeliveryBuilderCreatedLifecyclePhase;
import org.smooks.engine.lifecycle.ContentDeliveryConfigCreatedLifecyclePhase;
import org.smooks.engine.lifecycle.ContentHandlersCreatedLifecyclePhase;
import org.smooks.engine.lookup.ContentHandlerFactoryLookup;
import org.smooks.engine.lookup.GlobalParamsLookup;
import org.smooks.engine.lookup.InstanceLookup;
import org.smooks.engine.lookup.LifecycleManagerLookup;
import org.smooks.engine.lookup.ResourceConfigsProfileSetLookup;
import org.smooks.engine.resource.config.DefaultResourceConfigSortComparator;
import org.smooks.engine.resource.config.xpath.step.ElementSelectorStep;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Content delivery configuration builder.
 *
 * @author tfennelly
 */
public class DefaultContentDeliveryConfigBuilder implements ContentDeliveryConfigBuilder {

    protected static final String LINE_SEPARATOR = System.lineSeparator();

    /**
     * Logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultContentDeliveryConfigBuilder.class);

    /**
     * Profile set.
     */
    private final ProfileSet profileSet;

    private final Registry registry;

    /**
     * An unsorted list of ResourceConfig.
     */
    private final List<ResourceConfig> resourceConfigs = new ArrayList<>();
    /**
     * Table (by element) of sorted ResourceConfig instances keyed by selector value. Each table entry
     * contains a List of ResourceConfig instances.
     */
    private final Map<String, List<ResourceConfig>> resourceConfigTable = new LinkedHashMap<>();

    private final List<ContentHandlerBinding<Visitor>> visitorBindings = new ArrayList<>();

    /**
     * Config builder events list.
     */
    private final List<ContentDeliveryConfigExecutionEvent> configBuilderEvents = new ArrayList<>();
    private final List<FilterProvider> filterProviders;
    private final LifecycleManager lifecycleManager;

    private volatile ContentDeliveryConfig contentDeliveryConfig;

    protected enum Event {
        CONTENT_HANDLERS_CREATED,
        CONTENT_DELIVERY_BUILDER_CREATED,
        CONTENT_DELIVERY_CONFIG_CREATED
    }

    /**
     * Private (hidden) constructor.
     *
     * @param profileSet Profile set.
     * @param registry   Container context.
     */
    public DefaultContentDeliveryConfigBuilder(final ProfileSet profileSet, final Registry registry, final List<FilterProvider> filterProviders) {
        AssertArgument.isNotNull(profileSet, "profileSet");
        AssertArgument.isNotNull(registry, "registry");
        AssertArgument.isNotNull(filterProviders, "filterProviders");

        this.profileSet = profileSet;
        this.registry = registry;
        this.filterProviders = filterProviders;

        lifecycleManager = registry.lookup(new LifecycleManagerLookup());
    }

    /**
     * Get the ContentDeliveryConfig instance for the specified profile set.
     *
     * @param extendedContentHandlerBindings Preconfigured/extended Visitor Configuration Map.
     * @return The ContentDeliveryConfig instance for the named table.
     */
    @Override
    public ContentDeliveryConfig build(List<ContentHandlerBinding<Visitor>> extendedContentHandlerBindings) {
        if (contentDeliveryConfig == null) {
            synchronized (DefaultContentDeliveryConfigBuilder.class) {
                if (contentDeliveryConfig == null) {
                    load(profileSet);
                    fireEvent(Event.CONTENT_DELIVERY_BUILDER_CREATED);
                    contentDeliveryConfig = buildConfig(extendedContentHandlerBindings);
                }
            }
        }

        return contentDeliveryConfig;
    }

    protected ContentDeliveryConfig buildConfig(List<ContentHandlerBinding<Visitor>> extendedContentHandlerBindings) {
        if (extendedContentHandlerBindings != null) {
            visitorBindings.addAll(extendedContentHandlerBindings);
        }
        FilterProvider filterProvider = getFilterProvider();

        LOGGER.debug("Activating [{}] filter", filterProvider.getName());
        configBuilderEvents.add(new DefaultContentDeliveryConfigExecutionEvent("SAX/DOM support characteristics of the Resource Configuration map:" + LINE_SEPARATOR + getResourceFilterCharacteristics()));
        configBuilderEvents.add(new DefaultContentDeliveryConfigExecutionEvent(String.format("Activating [%s] filter", filterProvider.getName())));

        ContentDeliveryConfig contentDeliveryConfig = filterProvider.createContentDeliveryConfig(visitorBindings, registry, resourceConfigTable, configBuilderEvents);
        fireEvent(Event.CONTENT_DELIVERY_CONFIG_CREATED);

        return contentDeliveryConfig;
    }

    protected FilterProvider getFilterProvider() {
        final List<FilterProvider> candidateFilterProviders = filterProviders.stream().filter(s -> s.isProvider(visitorBindings)).collect(Collectors.toList());
        final String filterTypeParam = registry.lookup(new GlobalParamsLookup()).getParameterValue(Filter.STREAM_FILTER_TYPE);

        AnyFilterType anyFilterType = new AnyFilterType();
        if (filterTypeParam.equals(anyFilterType.getName()) && candidateFilterProviders.isEmpty()) {
            throw new SmooksConfigException("Ambiguous Resource Config set. All content handlers must support processing on the SAX and/or DOM Filter:" + LINE_SEPARATOR + getResourceFilterCharacteristics());
        } else if (filterTypeParam.equals(anyFilterType.getName())) {
            return candidateFilterProviders.get(0);
        } else {
            final Optional<FilterProvider> filterProviderOptional = candidateFilterProviders.stream().filter(c -> c.getName().equalsIgnoreCase(filterTypeParam)).findFirst();
            if (filterProviderOptional.isPresent()) {
                return filterProviderOptional.get();
            } else {
                throw new SmooksConfigException(String.format("The configured filter [%s] cannot be used: %s filters can be used for the given set of visitors. Turn on debug logging for more information", filterTypeParam, Arrays.toString(candidateFilterProviders.stream().map(FilterProvider::getName).collect(Collectors.toList()).toArray())));
            }
        }
    }

    /**
     * Logging support function.
     *
     * @return Verbose characteristics string.
     */
    protected String getResourceFilterCharacteristics() {
        StringBuffer stringBuf = new StringBuffer();
        List<ContentHandler> printedHandlers = new ArrayList<>();

        stringBuf.append("\t\tDOM   SAX    Resource  ('x' equals supported)" + LINE_SEPARATOR);
        stringBuf.append("\t\t---------------------------------------------------------------------" + LINE_SEPARATOR);

        for (ContentHandlerBinding<Visitor> contentHandlerBinding : visitorBindings) {
            printHandlerCharacteristics(contentHandlerBinding, stringBuf, printedHandlers);
        }

        stringBuf.append(LINE_SEPARATOR).append(LINE_SEPARATOR);

        return stringBuf.toString();
    }

    protected void printHandlerCharacteristics(ContentHandlerBinding<Visitor> contentHandlerBinding, StringBuffer stringBuf, List<ContentHandler> printedHandlers) {
        ContentHandler handler = contentHandlerBinding.getContentHandler();

        if (printedHandlers.contains(handler)) {
            return;
        } else {
            printedHandlers.add(handler);
        }

        stringBuf.append("\t\t ");
        Map<String, Boolean> supportedFilterProviders = getOrderedSupportedFilterProviders(contentHandlerBinding);
        for (Entry<String, Boolean> supportedFilterProvider : supportedFilterProviders.entrySet()) {
            stringBuf.append(supportedFilterProvider.getValue() ? supportedFilterProvider.getKey() : " ").append("     ");
        }

        stringBuf.append(contentHandlerBinding.getResourceConfig())
                .append(LINE_SEPARATOR);
    }

    private Map<String, Boolean> getOrderedSupportedFilterProviders(ContentHandlerBinding<Visitor> contentHandlerBinding) {
        Map<String, Boolean> supportedFilterProviders = new LinkedHashMap<>();
        filterProviders.forEach(
                s -> supportedFilterProviders.put(s.getName(), s.isProvider(Collections.singletonList(contentHandlerBinding)))
        );
        return supportedFilterProviders;
    }

    /**
     * Build the ContentDeliveryConfigBuilder for the specified device.
     * <p/>
     * Creates the buildTable instance and populates it with the ProcessingUnit matrix
     * for the specified device.
     */
    protected void load(ProfileSet profileSet) {
        resourceConfigs.clear();
        resourceConfigs.addAll(Arrays.asList(registry.lookup(new ResourceConfigsProfileSetLookup(registry, profileSet))));

        // Build and sort the resourceConfigTable table - non-transforming elements.
        buildResourceConfigTable(resourceConfigs);
        sortResourceConfigs(resourceConfigTable, profileSet);

        // Extract the ContentDeliveryUnits and build the tables
        extractContentHandlers();

        // Tell all interested listeners that all the handlers have now been created.
        fireEvent(Event.CONTENT_HANDLERS_CREATED);

        if (LOGGER.isDebugEnabled()) {
            logResourceConfig(profileSet);
        }
    }

    /**
     * Print a debug log of the resource configurations for the associated profile.
     */
    protected void logResourceConfig(ProfileSet profileSet) {
        LOGGER.debug("==================================================================================================");
        LOGGER.debug("Resource configuration (sorted) for profile [" + profileSet.getBaseProfile() + "].  Sub Profiles: [" + profileSet + "]");
        Iterator configurations = resourceConfigTable.entrySet().iterator();
        int i = 0;

        while (configurations.hasNext()) {
            Map.Entry entry = (Entry) configurations.next();
            List resources = (List) entry.getValue();

            LOGGER.debug(i + ") " + entry.getKey());
            for (int ii = 0; ii < resources.size(); ii++) {
                LOGGER.debug("\t(" + ii + ") " + resources.get(ii));
            }
        }
        LOGGER.debug("==================================================================================================");
    }

    /**
     * Build the basic ResourceConfig table from the list.
     *
     * @param resourceConfigs List of ResourceConfigs.
     */
    protected void buildResourceConfigTable(List<ResourceConfig> resourceConfigs) {
        for (final ResourceConfig resourceConfig : resourceConfigs) {
            addResourceConfig(resourceConfig);
        }
    }

    /**
     * Add the supplied resource configuration to this configuration's main
     * resource configuration list.
     *
     * @param config The configuration to be added.
     */
    protected void addResourceConfig(ResourceConfig config) {
        String target = config.getSelectorPath().getSelector();

        // If it's contextual, it's targeting an XML element...
        if (config.getSelectorPath().size() > 1) {
            for (int i = config.getSelectorPath().size(); i > 0; i--) {
                final SelectorStep selectorStep = config.getSelectorPath().get(i - 1);
                if (selectorStep instanceof ElementSelectorStep) {
                    target = ((ElementSelectorStep) selectorStep).getQName().getLocalPart();
                    break;
                }
            }
        }

        addResourceConfig(target, config);
    }

    /**
     * Add the config for the specified element.
     *
     * @param element        The element to which the config is to be added.
     * @param resourceConfig The Object to be added.
     */
    protected void addResourceConfig(String element, ResourceConfig resourceConfig) {
        // Add it to the unsorted list...
        if (!resourceConfigs.contains(resourceConfig)) {
            resourceConfigs.add(resourceConfig);
        }

        // Add it to the sorted resourceConfigTable...
        final List<ResourceConfig> resourceConfigs = resourceConfigTable.computeIfAbsent(element, k -> new ArrayList<>());
        if (!resourceConfigs.contains(resourceConfig)) {
            resourceConfigs.add(resourceConfig);
        }
    }

    /**
     * Iterate over the table smooks-resource instances and sort the ResourceConfigs
     * on each element.  Ordered by specificity.
     */
    @SuppressWarnings("unchecked")
    protected void sortResourceConfigs(Map<String, List<ResourceConfig>> table, ProfileSet profileSet) {
        String sortParam = registry.lookup(new GlobalParamsLookup()).getParameterValue("sort.resources");
        if (sortParam != null && sortParam.trim().equalsIgnoreCase("true")) {
            for (Entry<String, List<ResourceConfig>> entry : table.entrySet()) {
                List<ResourceConfig> markupElResourceConfigs = entry.getValue();
                ResourceConfig[] resourceConfigs = markupElResourceConfigs.toArray(new ResourceConfig[0]);
                ResourceConfigSortComparator sortComparator = new DefaultResourceConfigSortComparator(profileSet);

                Arrays.sort(resourceConfigs, sortComparator);
                entry.setValue(new ArrayList<>(Arrays.asList(resourceConfigs)));
            }
        }
    }

    /**
     * Extract the ContentHandler instances from the ResourceConfig table and add them to
     * their respective tables.
     */
    protected void extractContentHandlers() {
        ContentHandlerExtractionStrategy contentHandlerExtractionStrategy = new ContentHandlerExtractionStrategy(registry);
        ResourceConfigTableIterator resourceConfigTableIterator = new ResourceConfigTableIterator(contentHandlerExtractionStrategy);

        resourceConfigTableIterator.iterate();
    }

    protected void logExecutionEvent(ResourceConfig resourceConfig, String message) {
        configBuilderEvents.add(new DefaultContentDeliveryConfigExecutionEvent(resourceConfig, message));
    }

    protected void fireEvent(Event event) {
        final LifecyclePhase lifecyclePhase;
        switch (event) {
            case CONTENT_HANDLERS_CREATED:
                lifecyclePhase = new ContentHandlersCreatedLifecyclePhase();
                break;
            case CONTENT_DELIVERY_BUILDER_CREATED:
                lifecyclePhase = new ContentDeliveryBuilderCreatedLifecyclePhase();
                break;
            case CONTENT_DELIVERY_CONFIG_CREATED:
                lifecyclePhase = new ContentDeliveryConfigCreatedLifecyclePhase();
                break;
            default:
                throw new UnsupportedOperationException();
        }

        lifecycleManager.applyPhase(registry.lookup(new InstanceLookup<>(ContentDeliveryConfigLifecycle.class)).values(), lifecyclePhase);
    }

    /**
     * ContentHandler extraction strategy.
     *
     * @author tfennelly
     */
    private final class ContentHandlerExtractionStrategy implements ResourceConfigStrategy {

        private final Registry registry;

        private ContentHandlerExtractionStrategy(Registry registry) {
            this.registry = registry;
        }

        @Override
        public void applyStrategy(ResourceConfig resourceConfig) {
            applyContentDeliveryUnitStrategy(resourceConfig);
        }

        private boolean applyContentDeliveryUnitStrategy(final ResourceConfig resourceConfig) {
            // Try it as a Java class before trying anything else.  This is to
            // accommodate specification of the class in the standard
            // Java form e.g. java.lang.String Vs java/lang/String.class
            if (resourceConfig.isJavaResource()) {
                final ContentHandlerFactory<?> contentHandlerFactory = registry.lookup(new ContentHandlerFactoryLookup("class"));
                if (contentHandlerFactory == null) {
                    throw new SmooksConfigException(String.format("%s not found for content of type [class]. Hint: ensure the Smooks application context has the correct class loader set", ContentHandlerFactory.class.getName()));
                }
                // Job done - it's a CDU and we've added it!
                return addContentHandler(resourceConfig, contentHandlerFactory);

            } else {
                // Get the resource type and "try" creating a ContentHandlerFactory for that resource
                // type.
                final String resourceType = resourceConfig.getResourceType();
                final ContentHandlerFactory<?> contentHandlerFactory = tryCreateCreator(resourceType);

                // If we have a creator but it's the JavaContentHandlerFactory we ignore it because
                // we know the class in question does not implement ContentHandler.  We know because
                // we tried this above.
                if (contentHandlerFactory != null) {
                    if (!(contentHandlerFactory instanceof JavaContentHandlerFactory)) {
                        return addContentHandler(resourceConfig, contentHandlerFactory);
                    }
                } else {
                    // Just ignore it - something else will use it (hopefully)
                    if (resourceType != null) {
                        logExecutionEvent(resourceConfig, "Unable to create ContentHandler class instance for resource.  " +
                                "This is probably because there's no " + ContentHandlerFactory.class.getSimpleName() + " implementation for resource " +
                                "type '" + resourceType + "' available on the classpath.");
                    }
                }
            }

            return false;
        }

        /**
         * Try create the CDU creator for the specified resource type.
         * <p/>
         * Return null if unsuccessful i.e. no exceptions.
         *
         * @param resourceType The resource type.
         * @return The appropriate CDU creator instance, or null if there is none.
         */
        private ContentHandlerFactory<?> tryCreateCreator(String resourceType) {
            if (resourceType == null || resourceType.trim().isEmpty()) {
                LOGGER.debug("Request to attempt ContentHandlerFactory creation based on a null/empty resource type.");
                return null;
            }

            return registry.lookup(new ContentHandlerFactoryLookup(resourceType));

        }

        /**
         * Add a {@link ContentHandler} for the specified element and configuration.
         *
         * @param resourceConfig        Configuration.
         * @param contentHandlerFactory CDU Creator class.
         * @return True if the CDU was added, otherwise false.
         */
        private boolean addContentHandler(ResourceConfig resourceConfig, ContentHandlerFactory<?> contentHandlerFactory) {
            Object contentHandler;

            // Create the ContentHandler.
            try {
                contentHandler = contentHandlerFactory.create(resourceConfig);
            } catch (SmooksConfigException e) {
                throw e;
            } catch (Throwable thrown) {
                LOGGER.error("ContentHandlerFactory [{}] unable to create content handler for resource [{}] ", contentHandlerFactory.getClass().getName(), resourceConfig);
                throw thrown;
            }

            //TODO: register object

            if (contentHandler instanceof Visitor) {
                // Add the visitor.  No need to configure it as that should have been done by
                // creator...
                visitorBindings.add(new DefaultContentHandlerBinding<>((Visitor) contentHandler, resourceConfig));
            }

            // Content delivery units are allowed to dynamically add new configurations...
            if (contentHandler instanceof ResourceConfigExpander) {
                List<ResourceConfig> additionalConfigs = ((ResourceConfigExpander) contentHandler).expandConfigurations();
                if (additionalConfigs != null && !additionalConfigs.isEmpty()) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Adding expansion resource configurations created by: " + resourceConfig);
                        for (ResourceConfig additionalConfig : additionalConfigs) {
                            LOGGER.debug("\tAdding expansion resource configuration: " + additionalConfig);
                        }
                    }
                    processExpansionConfigurations(additionalConfigs);
                }
            }

            if (contentHandler instanceof VisitorAppender) {
                visitorBindings.addAll(((VisitorAppender) contentHandler).addVisitors());
            }

            return true;
        }

        /**
         * Process the supplied expansion configurations.
         *
         * @param additionalConfigs Expansion configs.
         */
        private void processExpansionConfigurations(List<ResourceConfig> additionalConfigs) {
            for (final ResourceConfig resourceConfig : additionalConfigs) {
                registry.registerResourceConfig(resourceConfig);
                // Try adding it as a ContentHandler instance...
                if (!applyContentDeliveryUnitStrategy(resourceConfig)) {
                    // Else just add it to the main list...
                    addResourceConfig(resourceConfig);
                }
            }
        }

    }

    /**
     * Iterate over the ResourceConfig table applying the constructor
     * supplied ResourceConfigStrategy.
     *
     * @author tfennelly
     */
    private class ResourceConfigTableIterator {

        /**
         * Iteration strategy.
         */
        private final ResourceConfigStrategy strategy;

        /**
         * Private constructor.
         *
         * @param strategy Strategy algorithm implementation.
         */
        private ResourceConfigTableIterator(ResourceConfigStrategy strategy) {
            this.strategy = strategy;
        }

        /**
         * Iterate over the table applying the strategy.
         */
        private void iterate() {
            for (ResourceConfig resourceConfig : resourceConfigs) {
                strategy.applyStrategy(resourceConfig);
            }
        }
    }

    /**
     * Unitdef iteration strategy interface.
     *
     * @author tfennelly
     */
    private interface ResourceConfigStrategy {
        /**
         * Apply the strategy algorithm.
         *
         * @param resourceConfig The ResourceConfig
         */
        void applyStrategy(ResourceConfig resourceConfig);
    }
}

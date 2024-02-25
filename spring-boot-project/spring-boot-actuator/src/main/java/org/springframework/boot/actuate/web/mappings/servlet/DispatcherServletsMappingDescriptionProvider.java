/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.web.mappings.servlet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import jakarta.servlet.Servlet;

import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.actuate.web.mappings.HandlerMethodDescription;
import org.springframework.boot.actuate.web.mappings.MappingDescriptionProvider;
import org.springframework.boot.actuate.web.mappings.servlet.DispatcherServletsMappingDescriptionProvider.DispatcherServletsMappingDescriptionProviderRuntimeHints;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.AbstractUrlHandlerMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;

/**
 * A {@link MappingDescriptionProvider} that introspects the {@link HandlerMapping
 * HandlerMappings} that are known to one or more {@link DispatcherServlet
 * DispatcherServlets}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@ImportRuntimeHints(DispatcherServletsMappingDescriptionProviderRuntimeHints.class)
public class DispatcherServletsMappingDescriptionProvider implements MappingDescriptionProvider {

	private static final List<HandlerMappingDescriptionProvider<?>> descriptionProviders;

	static {
		List<HandlerMappingDescriptionProvider<?>> providers = new ArrayList<>();
		providers.add(new RequestMappingInfoHandlerMappingDescriptionProvider());
		providers.add(new UrlHandlerMappingDescriptionProvider());
		providers.add(new IterableDelegatesHandlerMappingDescriptionProvider(new ArrayList<>(providers)));
		descriptionProviders = Collections.unmodifiableList(providers);
	}

	/**
     * Returns the mapping name for the DispatcherServlets.
     *
     * @return the mapping name for the DispatcherServlets
     */
    @Override
	public String getMappingName() {
		return "dispatcherServlets";
	}

	/**
     * Returns a map of servlet mappings descriptions for the given application context.
     * 
     * @param context the application context to describe mappings for
     * @return a map of servlet mappings descriptions
     */
    @Override
	public Map<String, List<DispatcherServletMappingDescription>> describeMappings(ApplicationContext context) {
		if (context instanceof WebApplicationContext webApplicationContext) {
			return describeMappings(webApplicationContext);
		}
		return Collections.emptyMap();
	}

	/**
     * Returns a map of dispatcher servlet mappings for each dispatcher servlet in the given web application context.
     * 
     * @param context the web application context
     * @return a map of dispatcher servlet mappings, where the key is the name of the dispatcher servlet and the value is a list of DispatcherServletMappingDescription objects
     */
    private Map<String, List<DispatcherServletMappingDescription>> describeMappings(WebApplicationContext context) {
		Map<String, List<DispatcherServletMappingDescription>> mappings = new HashMap<>();
		determineDispatcherServlets(context).forEach((name, dispatcherServlet) -> mappings.put(name,
				describeMappings(new DispatcherServletHandlerMappings(name, dispatcherServlet, context))));
		return mappings;
	}

	/**
     * Determines the dispatcher servlets in the given web application context.
     * 
     * @param context the web application context
     * @return a map of dispatcher servlets, where the key is the servlet name and the value is the dispatcher servlet instance
     */
    private Map<String, DispatcherServlet> determineDispatcherServlets(WebApplicationContext context) {
		Map<String, DispatcherServlet> dispatcherServlets = new LinkedHashMap<>();
		context.getBeansOfType(ServletRegistrationBean.class).values().forEach((registration) -> {
			Servlet servlet = registration.getServlet();
			if (servlet instanceof DispatcherServlet && !dispatcherServlets.containsValue(servlet)) {
				dispatcherServlets.put(registration.getServletName(), (DispatcherServlet) servlet);
			}
		});
		context.getBeansOfType(DispatcherServlet.class).forEach((name, dispatcherServlet) -> {
			if (!dispatcherServlets.containsValue(dispatcherServlet)) {
				dispatcherServlets.put(name, dispatcherServlet);
			}
		});
		return dispatcherServlets;
	}

	/**
     * Returns a list of DispatcherServletMappingDescription objects that describe the mappings
     * in the given DispatcherServletHandlerMappings.
     *
     * @param mappings the DispatcherServletHandlerMappings to describe
     * @return a list of DispatcherServletMappingDescription objects
     */
    private List<DispatcherServletMappingDescription> describeMappings(DispatcherServletHandlerMappings mappings) {
		return mappings.getHandlerMappings().stream().flatMap(this::describe).toList();
	}

	/**
     * Returns a stream of DispatcherServletMappingDescription objects that describe the given handler mapping.
     * 
     * @param handlerMapping the handler mapping to describe
     * @param descriptionProviders the list of description providers to use
     * @param <T> the type of the handler mapping
     * @return a stream of DispatcherServletMappingDescription objects
     */
    private <T> Stream<DispatcherServletMappingDescription> describe(T handlerMapping) {
		return describe(handlerMapping, descriptionProviders).stream();
	}

	/**
     * Describes the given handler mapping using the provided description providers.
     * 
     * @param handlerMapping         the handler mapping to describe
     * @param descriptionProviders   the list of description providers to use
     * @return                       a list of DispatcherServletMappingDescription objects describing the handler mapping
     */
    @SuppressWarnings("unchecked")
	private static <T> List<DispatcherServletMappingDescription> describe(T handlerMapping,
			List<HandlerMappingDescriptionProvider<?>> descriptionProviders) {
		for (HandlerMappingDescriptionProvider<?> descriptionProvider : descriptionProviders) {
			if (descriptionProvider.getMappingClass().isInstance(handlerMapping)) {
				return ((HandlerMappingDescriptionProvider<T>) descriptionProvider).describe(handlerMapping);
			}
		}
		return Collections.emptyList();
	}

	private interface HandlerMappingDescriptionProvider<T> {

		Class<T> getMappingClass();

		List<DispatcherServletMappingDescription> describe(T handlerMapping);

	}

	/**
     * RequestMappingInfoHandlerMappingDescriptionProvider class.
     */
    private static final class RequestMappingInfoHandlerMappingDescriptionProvider
			implements HandlerMappingDescriptionProvider<RequestMappingInfoHandlerMapping> {

		/**
         * Returns the mapping class for the RequestMappingInfoHandlerMapping.
         * 
         * @return the mapping class for the RequestMappingInfoHandlerMapping
         */
        @Override
		public Class<RequestMappingInfoHandlerMapping> getMappingClass() {
			return RequestMappingInfoHandlerMapping.class;
		}

		/**
         * Returns a list of DispatcherServletMappingDescription objects that describe the handler methods
         * registered in the provided RequestMappingInfoHandlerMapping.
         *
         * @param handlerMapping the RequestMappingInfoHandlerMapping containing the registered handler methods
         * @return a list of DispatcherServletMappingDescription objects describing the handler methods
         */
        @Override
		public List<DispatcherServletMappingDescription> describe(RequestMappingInfoHandlerMapping handlerMapping) {
			Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();
			return handlerMethods.entrySet().stream().map(this::describe).toList();
		}

		/**
         * Generates a description of the DispatcherServlet mapping based on the provided mapping entry.
         * 
         * @param mapping the mapping entry containing the RequestMappingInfo and HandlerMethod
         * @return a DispatcherServletMappingDescription object containing the details of the mapping
         */
        private DispatcherServletMappingDescription describe(Entry<RequestMappingInfo, HandlerMethod> mapping) {
			DispatcherServletMappingDetails mappingDetails = new DispatcherServletMappingDetails();
			mappingDetails.setHandlerMethod(new HandlerMethodDescription(mapping.getValue()));
			mappingDetails.setRequestMappingConditions(new RequestMappingConditionsDescription(mapping.getKey()));
			return new DispatcherServletMappingDescription(mapping.getKey().toString(), mapping.getValue().toString(),
					mappingDetails);
		}

	}

	/**
     * UrlHandlerMappingDescriptionProvider class.
     */
    private static final class UrlHandlerMappingDescriptionProvider
			implements HandlerMappingDescriptionProvider<AbstractUrlHandlerMapping> {

		/**
         * Returns the mapping class for the UrlHandlerMapping.
         * 
         * @return the mapping class for the UrlHandlerMapping
         */
        @Override
		public Class<AbstractUrlHandlerMapping> getMappingClass() {
			return AbstractUrlHandlerMapping.class;
		}

		/**
         * Returns a list of DispatcherServletMappingDescription objects that describe the given handler mapping.
         *
         * @param handlerMapping the AbstractUrlHandlerMapping to describe
         * @return a list of DispatcherServletMappingDescription objects
         */
        @Override
		public List<DispatcherServletMappingDescription> describe(AbstractUrlHandlerMapping handlerMapping) {
			return handlerMapping.getHandlerMap().entrySet().stream().map(this::describe).toList();
		}

		/**
         * Generates a description for a DispatcherServlet mapping.
         * 
         * @param mapping the mapping entry containing the URL pattern and the corresponding handler object
         * @return a DispatcherServletMappingDescription object representing the mapping
         */
        private DispatcherServletMappingDescription describe(Entry<String, Object> mapping) {
			return new DispatcherServletMappingDescription(mapping.getKey(), mapping.getValue().toString(), null);
		}

	}

	/**
     * IterableDelegatesHandlerMappingDescriptionProvider class.
     */
    @SuppressWarnings("rawtypes")
	private static final class IterableDelegatesHandlerMappingDescriptionProvider
			implements HandlerMappingDescriptionProvider<Iterable> {

		private final List<HandlerMappingDescriptionProvider<?>> descriptionProviders;

		/**
         * Constructs a new IterableDelegatesHandlerMappingDescriptionProvider with the specified list of description providers.
         * 
         * @param descriptionProviders the list of HandlerMappingDescriptionProvider instances to be used by this IterableDelegatesHandlerMappingDescriptionProvider
         */
        private IterableDelegatesHandlerMappingDescriptionProvider(
				List<HandlerMappingDescriptionProvider<?>> descriptionProviders) {
			this.descriptionProviders = descriptionProviders;
		}

		/**
         * Returns the mapping class for the IterableDelegatesHandlerMappingDescriptionProvider.
         * 
         * @return the mapping class for the IterableDelegatesHandlerMappingDescriptionProvider
         */
        @Override
		public Class<Iterable> getMappingClass() {
			return Iterable.class;
		}

		/**
         * Generates a list of DispatcherServletMappingDescription objects by describing the given handler mappings.
         * 
         * @param handlerMapping the iterable collection of handler mappings to describe
         * @return a list of DispatcherServletMappingDescription objects describing the handler mappings
         */
        @Override
		public List<DispatcherServletMappingDescription> describe(Iterable handlerMapping) {
			List<DispatcherServletMappingDescription> descriptions = new ArrayList<>();
			for (Object delegate : handlerMapping) {
				descriptions
					.addAll(DispatcherServletsMappingDescriptionProvider.describe(delegate, this.descriptionProviders));
			}
			return descriptions;
		}

	}

	/**
     * DispatcherServletsMappingDescriptionProviderRuntimeHints class.
     */
    static class DispatcherServletsMappingDescriptionProviderRuntimeHints implements RuntimeHintsRegistrar {

		private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();

		/**
         * Registers the runtime hints for the DispatcherServletsMappingDescriptionProvider.
         * 
         * @param hints the runtime hints to register
         * @param classLoader the class loader to use for reflection
         */
        @Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			this.bindingRegistrar.registerReflectionHints(hints.reflection(),
					DispatcherServletMappingDescription.class);
		}

	}

}

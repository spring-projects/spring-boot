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

	@Override
	public String getMappingName() {
		return "dispatcherServlets";
	}

	@Override
	public Map<String, List<DispatcherServletMappingDescription>> describeMappings(ApplicationContext context) {
		if (context instanceof WebApplicationContext webApplicationContext) {
			return describeMappings(webApplicationContext);
		}
		return Collections.emptyMap();
	}

	private Map<String, List<DispatcherServletMappingDescription>> describeMappings(WebApplicationContext context) {
		Map<String, List<DispatcherServletMappingDescription>> mappings = new HashMap<>();
		determineDispatcherServlets(context).forEach((name, dispatcherServlet) -> mappings.put(name,
				describeMappings(new DispatcherServletHandlerMappings(name, dispatcherServlet, context))));
		return mappings;
	}

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

	private List<DispatcherServletMappingDescription> describeMappings(DispatcherServletHandlerMappings mappings) {
		return mappings.getHandlerMappings().stream().flatMap(this::describe).toList();
	}

	private <T> Stream<DispatcherServletMappingDescription> describe(T handlerMapping) {
		return describe(handlerMapping, descriptionProviders).stream();
	}

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

	private static final class RequestMappingInfoHandlerMappingDescriptionProvider
			implements HandlerMappingDescriptionProvider<RequestMappingInfoHandlerMapping> {

		@Override
		public Class<RequestMappingInfoHandlerMapping> getMappingClass() {
			return RequestMappingInfoHandlerMapping.class;
		}

		@Override
		public List<DispatcherServletMappingDescription> describe(RequestMappingInfoHandlerMapping handlerMapping) {
			Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();
			return handlerMethods.entrySet().stream().map(this::describe).toList();
		}

		private DispatcherServletMappingDescription describe(Entry<RequestMappingInfo, HandlerMethod> mapping) {
			DispatcherServletMappingDetails mappingDetails = new DispatcherServletMappingDetails();
			mappingDetails.setHandlerMethod(new HandlerMethodDescription(mapping.getValue()));
			mappingDetails.setRequestMappingConditions(new RequestMappingConditionsDescription(mapping.getKey()));
			return new DispatcherServletMappingDescription(mapping.getKey().toString(), mapping.getValue().toString(),
					mappingDetails);
		}

	}

	private static final class UrlHandlerMappingDescriptionProvider
			implements HandlerMappingDescriptionProvider<AbstractUrlHandlerMapping> {

		@Override
		public Class<AbstractUrlHandlerMapping> getMappingClass() {
			return AbstractUrlHandlerMapping.class;
		}

		@Override
		public List<DispatcherServletMappingDescription> describe(AbstractUrlHandlerMapping handlerMapping) {
			return handlerMapping.getHandlerMap().entrySet().stream().map(this::describe).toList();
		}

		private DispatcherServletMappingDescription describe(Entry<String, Object> mapping) {
			return new DispatcherServletMappingDescription(mapping.getKey(), mapping.getValue().toString(), null);
		}

	}

	@SuppressWarnings("rawtypes")
	private static final class IterableDelegatesHandlerMappingDescriptionProvider
			implements HandlerMappingDescriptionProvider<Iterable> {

		private final List<HandlerMappingDescriptionProvider<?>> descriptionProviders;

		private IterableDelegatesHandlerMappingDescriptionProvider(
				List<HandlerMappingDescriptionProvider<?>> descriptionProviders) {
			this.descriptionProviders = descriptionProviders;
		}

		@Override
		public Class<Iterable> getMappingClass() {
			return Iterable.class;
		}

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

	static class DispatcherServletsMappingDescriptionProviderRuntimeHints implements RuntimeHintsRegistrar {

		private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			this.bindingRegistrar.registerReflectionHints(hints.reflection(),
					DispatcherServletMappingDescription.class);
		}

	}

}

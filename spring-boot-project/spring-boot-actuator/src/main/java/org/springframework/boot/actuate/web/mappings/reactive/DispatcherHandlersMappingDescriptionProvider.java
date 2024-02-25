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

package org.springframework.boot.actuate.web.mappings.reactive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Stream;

import reactor.core.publisher.Mono;

import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.actuate.web.mappings.HandlerMethodDescription;
import org.springframework.boot.actuate.web.mappings.MappingDescriptionProvider;
import org.springframework.boot.actuate.web.mappings.reactive.DispatcherHandlersMappingDescriptionProvider.DispatcherHandlersMappingDescriptionProviderRuntimeHints;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.io.Resource;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RequestPredicate;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions.Visitor;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.support.RouterFunctionMapping;
import org.springframework.web.reactive.handler.AbstractUrlHandlerMapping;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
import org.springframework.web.reactive.result.method.RequestMappingInfoHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

/**
 * A {@link MappingDescriptionProvider} that introspects the {@link HandlerMapping
 * HandlerMappings} that are known to a {@link DispatcherHandler}.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@ImportRuntimeHints(DispatcherHandlersMappingDescriptionProviderRuntimeHints.class)
public class DispatcherHandlersMappingDescriptionProvider implements MappingDescriptionProvider {

	private static final List<HandlerMappingDescriptionProvider<? extends HandlerMapping>> descriptionProviders = Arrays
		.asList(new RequestMappingInfoHandlerMappingDescriptionProvider(), new UrlHandlerMappingDescriptionProvider(),
				new RouterFunctionMappingDescriptionProvider());

	/**
	 * Returns the mapping name for the DispatcherHandlers.
	 * @return the mapping name for the DispatcherHandlers
	 */
	@Override
	public String getMappingName() {
		return "dispatcherHandlers";
	}

	/**
	 * Returns a map of handler names to their corresponding list of
	 * DispatcherHandlerMappingDescriptions.
	 * @param context the ApplicationContext to retrieve the DispatcherHandlers from
	 * @return a map of handler names to their corresponding list of
	 * DispatcherHandlerMappingDescriptions
	 */
	@Override
	public Map<String, List<DispatcherHandlerMappingDescription>> describeMappings(ApplicationContext context) {
		Map<String, List<DispatcherHandlerMappingDescription>> mappings = new HashMap<>();
		context.getBeansOfType(DispatcherHandler.class)
			.forEach((name, handler) -> mappings.put(name, describeMappings(handler)));
		return mappings;
	}

	/**
	 * Returns a list of DispatcherHandlerMappingDescription objects that describe the
	 * mappings of the given DispatcherHandler.
	 * @param dispatcherHandler the DispatcherHandler whose mappings need to be described
	 * @return a list of DispatcherHandlerMappingDescription objects describing the
	 * mappings of the given DispatcherHandler
	 */
	private List<DispatcherHandlerMappingDescription> describeMappings(DispatcherHandler dispatcherHandler) {
		return dispatcherHandler.getHandlerMappings().stream().flatMap(this::describe).toList();
	}

	/**
	 * Describes a handler mapping by iterating through a list of description providers
	 * and returning the description provided by the matching provider.
	 * @param handlerMapping the handler mapping to be described
	 * @param <T> the type of the handler mapping
	 * @return a stream of DispatcherHandlerMappingDescription objects representing the
	 * description of the handler mapping
	 */
	@SuppressWarnings("unchecked")
	private <T extends HandlerMapping> Stream<DispatcherHandlerMappingDescription> describe(T handlerMapping) {
		for (HandlerMappingDescriptionProvider<?> descriptionProvider : descriptionProviders) {
			if (descriptionProvider.getMappingClass().isInstance(handlerMapping)) {
				return ((HandlerMappingDescriptionProvider<T>) descriptionProvider).describe(handlerMapping).stream();
			}
		}
		return Stream.empty();
	}

	private interface HandlerMappingDescriptionProvider<T extends HandlerMapping> {

		Class<T> getMappingClass();

		List<DispatcherHandlerMappingDescription> describe(T handlerMapping);

	}

	/**
	 * RequestMappingInfoHandlerMappingDescriptionProvider class.
	 */
	private static final class RequestMappingInfoHandlerMappingDescriptionProvider
			implements HandlerMappingDescriptionProvider<RequestMappingInfoHandlerMapping> {

		/**
		 * Returns the mapping class for the RequestMappingInfoHandlerMapping.
		 * @return the mapping class for the RequestMappingInfoHandlerMapping
		 */
		@Override
		public Class<RequestMappingInfoHandlerMapping> getMappingClass() {
			return RequestMappingInfoHandlerMapping.class;
		}

		/**
		 * Returns a list of DispatcherHandlerMappingDescription objects that describe the
		 * handler methods in the given RequestMappingInfoHandlerMapping.
		 * @param handlerMapping the RequestMappingInfoHandlerMapping to describe
		 * @return a list of DispatcherHandlerMappingDescription objects
		 */
		@Override
		public List<DispatcherHandlerMappingDescription> describe(RequestMappingInfoHandlerMapping handlerMapping) {
			Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();
			return handlerMethods.entrySet().stream().map(this::describe).toList();
		}

		/**
		 * Generates a description for a given mapping entry.
		 * @param mapping the mapping entry to describe
		 * @return the description of the mapping entry
		 */
		private DispatcherHandlerMappingDescription describe(Entry<RequestMappingInfo, HandlerMethod> mapping) {
			DispatcherHandlerMappingDetails handlerMapping = new DispatcherHandlerMappingDetails();
			handlerMapping.setHandlerMethod(new HandlerMethodDescription(mapping.getValue()));
			handlerMapping.setRequestMappingConditions(new RequestMappingConditionsDescription(mapping.getKey()));
			return new DispatcherHandlerMappingDescription(mapping.getKey().toString(), mapping.getValue().toString(),
					handlerMapping);
		}

	}

	/**
	 * UrlHandlerMappingDescriptionProvider class.
	 */
	private static final class UrlHandlerMappingDescriptionProvider
			implements HandlerMappingDescriptionProvider<AbstractUrlHandlerMapping> {

		/**
		 * Returns the mapping class for the UrlHandlerMapping.
		 * @return the mapping class for the UrlHandlerMapping
		 */
		@Override
		public Class<AbstractUrlHandlerMapping> getMappingClass() {
			return AbstractUrlHandlerMapping.class;
		}

		/**
		 * Returns a list of DispatcherHandlerMappingDescription objects that describe the
		 * given handler mapping.
		 * @param handlerMapping the AbstractUrlHandlerMapping to describe
		 * @return a list of DispatcherHandlerMappingDescription objects
		 */
		@Override
		public List<DispatcherHandlerMappingDescription> describe(AbstractUrlHandlerMapping handlerMapping) {
			return handlerMapping.getHandlerMap().entrySet().stream().map(this::describe).toList();
		}

		/**
		 * Generates a description for a given mapping entry in the form of a
		 * DispatcherHandlerMappingDescription object.
		 * @param mapping the mapping entry to describe
		 * @return a DispatcherHandlerMappingDescription object containing the description
		 * of the mapping
		 */
		private DispatcherHandlerMappingDescription describe(Entry<PathPattern, Object> mapping) {
			return new DispatcherHandlerMappingDescription(mapping.getKey().getPatternString(),
					mapping.getValue().toString(), null);
		}

	}

	/**
	 * RouterFunctionMappingDescriptionProvider class.
	 */
	private static final class RouterFunctionMappingDescriptionProvider
			implements HandlerMappingDescriptionProvider<RouterFunctionMapping> {

		/**
		 * Returns the mapping class for the RouterFunctionMapping.
		 * @return the mapping class for the RouterFunctionMapping
		 */
		@Override
		public Class<RouterFunctionMapping> getMappingClass() {
			return RouterFunctionMapping.class;
		}

		/**
		 * Generates a list of DispatcherHandlerMappingDescription objects that describe
		 * the given RouterFunctionMapping.
		 * @param handlerMapping The RouterFunctionMapping to describe.
		 * @return A list of DispatcherHandlerMappingDescription objects.
		 */
		@Override
		public List<DispatcherHandlerMappingDescription> describe(RouterFunctionMapping handlerMapping) {
			MappingDescriptionVisitor visitor = new MappingDescriptionVisitor();
			RouterFunction<?> routerFunction = handlerMapping.getRouterFunction();
			if (routerFunction != null) {
				routerFunction.accept(visitor);
			}
			return visitor.descriptions;
		}

	}

	/**
	 * MappingDescriptionVisitor class.
	 */
	private static final class MappingDescriptionVisitor implements Visitor {

		private final List<DispatcherHandlerMappingDescription> descriptions = new ArrayList<>();

		/**
		 * Starts a nested mapping description with the given request predicate.
		 * @param predicate the request predicate to be used for the nested mapping
		 * description
		 */
		@Override
		public void startNested(RequestPredicate predicate) {
		}

		/**
		 * Ends a nested request predicate.
		 * @param predicate the request predicate to end
		 */
		@Override
		public void endNested(RequestPredicate predicate) {
		}

		/**
		 * Registers a route with the given predicate and handler function.
		 * @param predicate the request predicate to match against
		 * @param handlerFunction the handler function to execute for matching requests
		 */
		@Override
		public void route(RequestPredicate predicate, HandlerFunction<?> handlerFunction) {
			DispatcherHandlerMappingDetails details = new DispatcherHandlerMappingDetails();
			details.setHandlerFunction(new HandlerFunctionDescription(handlerFunction));
			this.descriptions.add(
					new DispatcherHandlerMappingDescription(predicate.toString(), handlerFunction.toString(), details));
		}

		/**
		 * Sets the resources for the MappingDescriptionVisitor.
		 * @param lookupFunction the function used to lookup resources based on the server
		 * request
		 */
		@Override
		public void resources(Function<ServerRequest, Mono<Resource>> lookupFunction) {
		}

		/**
		 * Sets the attributes for the mapping description.
		 * @param attributes a map containing the attributes to be set
		 */
		@Override
		public void attributes(Map<String, Object> attributes) {
		}

		/**
		 * This method is used to handle unknown router functions.
		 * @param routerFunction the router function to be handled
		 */
		@Override
		public void unknown(RouterFunction<?> routerFunction) {
		}

	}

	/**
	 * DispatcherHandlersMappingDescriptionProviderRuntimeHints class.
	 */
	static class DispatcherHandlersMappingDescriptionProviderRuntimeHints implements RuntimeHintsRegistrar {

		private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();

		/**
		 * Registers the runtime hints for the
		 * DispatcherHandlersMappingDescriptionProvider class.
		 * @param hints The runtime hints to register.
		 * @param classLoader The class loader to use for registering the hints.
		 */
		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			this.bindingRegistrar.registerReflectionHints(hints.reflection(),
					DispatcherHandlerMappingDescription.class);
		}

	}

}

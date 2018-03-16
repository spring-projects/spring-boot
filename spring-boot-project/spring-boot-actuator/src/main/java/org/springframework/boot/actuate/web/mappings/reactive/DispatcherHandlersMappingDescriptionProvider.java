/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.web.mappings.HandlerMethodDescription;
import org.springframework.boot.actuate.web.mappings.MappingDescriptionProvider;
import org.springframework.context.ApplicationContext;
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
public class DispatcherHandlersMappingDescriptionProvider
		implements MappingDescriptionProvider {

	private static final List<HandlerMappingDescriptionProvider<? extends HandlerMapping>> descriptionProviders = Arrays
			.asList(new RequestMappingInfoHandlerMappingDescriptionProvider(),
					new UrlHandlerMappingDescriptionProvider(),
					new RouterFunctionMappingDescriptionProvider());

	@Override
	public String getMappingName() {
		return "dispatcherHandlers";
	}

	@Override
	public Map<String, List<DispatcherHandlerMappingDescription>> describeMappings(
			ApplicationContext context) {
		Map<String, List<DispatcherHandlerMappingDescription>> mappings = new HashMap<>();
		context.getBeansOfType(DispatcherHandler.class).forEach(
				(name, handler) -> mappings.put(name, describeMappings(handler)));
		return mappings;
	}

	private List<DispatcherHandlerMappingDescription> describeMappings(
			DispatcherHandler dispatcherHandler) {
		return dispatcherHandler.getHandlerMappings().stream().flatMap(this::describe)
				.collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	private <T extends HandlerMapping> Stream<DispatcherHandlerMappingDescription> describe(
			T handlerMapping) {
		for (HandlerMappingDescriptionProvider<?> descriptionProvider : descriptionProviders) {
			if (descriptionProvider.getMappingClass().isInstance(handlerMapping)) {
				return ((HandlerMappingDescriptionProvider<T>) descriptionProvider)
						.describe(handlerMapping).stream();

			}
		}
		return Stream.empty();
	}

	private interface HandlerMappingDescriptionProvider<T extends HandlerMapping> {

		Class<T> getMappingClass();

		List<DispatcherHandlerMappingDescription> describe(T handlerMapping);

	}

	private static final class RequestMappingInfoHandlerMappingDescriptionProvider
			implements
			HandlerMappingDescriptionProvider<RequestMappingInfoHandlerMapping> {

		@Override
		public Class<RequestMappingInfoHandlerMapping> getMappingClass() {
			return RequestMappingInfoHandlerMapping.class;
		}

		@Override
		public List<DispatcherHandlerMappingDescription> describe(
				RequestMappingInfoHandlerMapping handlerMapping) {
			Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping
					.getHandlerMethods();
			return handlerMethods.entrySet().stream().map(this::describe)
					.collect(Collectors.toList());
		}

		private DispatcherHandlerMappingDescription describe(
				Entry<RequestMappingInfo, HandlerMethod> mapping) {
			DispatcherHandlerMappingDetails handlerMapping = new DispatcherHandlerMappingDetails();
			handlerMapping
					.setHandlerMethod(new HandlerMethodDescription(mapping.getValue()));
			handlerMapping.setRequestMappingConditions(
					new RequestMappingConditionsDescription(mapping.getKey()));
			return new DispatcherHandlerMappingDescription(mapping.getKey().toString(),
					mapping.getValue().toString(), handlerMapping);
		}

	}

	private static final class UrlHandlerMappingDescriptionProvider
			implements HandlerMappingDescriptionProvider<AbstractUrlHandlerMapping> {

		@Override
		public Class<AbstractUrlHandlerMapping> getMappingClass() {
			return AbstractUrlHandlerMapping.class;
		}

		@Override
		public List<DispatcherHandlerMappingDescription> describe(
				AbstractUrlHandlerMapping handlerMapping) {
			return handlerMapping.getHandlerMap().entrySet().stream().map(this::describe)
					.collect(Collectors.toList());
		}

		private DispatcherHandlerMappingDescription describe(
				Entry<PathPattern, Object> mapping) {
			return new DispatcherHandlerMappingDescription(
					mapping.getKey().getPatternString(), mapping.getValue().toString(),
					null);
		}

	}

	private static final class RouterFunctionMappingDescriptionProvider
			implements HandlerMappingDescriptionProvider<RouterFunctionMapping> {

		@Override
		public Class<RouterFunctionMapping> getMappingClass() {
			return RouterFunctionMapping.class;
		}

		@Override
		public List<DispatcherHandlerMappingDescription> describe(
				RouterFunctionMapping handlerMapping) {
			MappingDescriptionVisitor visitor = new MappingDescriptionVisitor();
			RouterFunction<?> routerFunction = handlerMapping.getRouterFunction();
			if (routerFunction != null) {
				routerFunction.accept(visitor);
			}
			return visitor.descriptions;
		}

	}

	private static final class MappingDescriptionVisitor implements Visitor {

		private final List<DispatcherHandlerMappingDescription> descriptions = new ArrayList<>();

		@Override
		public void startNested(RequestPredicate predicate) {
		}

		@Override
		public void endNested(RequestPredicate predicate) {
		}

		@Override
		public void route(RequestPredicate predicate,
				HandlerFunction<?> handlerFunction) {
			DispatcherHandlerMappingDetails details = new DispatcherHandlerMappingDetails();
			details.setHandlerFunction(new HandlerFunctionDescription(handlerFunction));
			this.descriptions.add(new DispatcherHandlerMappingDescription(
					predicate.toString(), handlerFunction.toString(), details));
		}

		@Override
		public void resources(Function<ServerRequest, Mono<Resource>> lookupFunction) {
		}

		@Override
		public void unknown(RouterFunction<?> routerFunction) {
		}

	}

}

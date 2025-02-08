/*
 * Copyright 2012-2025 the original author or authors.
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


import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RequestPredicate;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;


import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;

import reactor.core.publisher.Mono;


/**
 * A Visitor that builds a list of {@link DispatcherHandlerMappingDescription DispatcherHandlerMappingDescriptions}
 * @since 3.5.0
 * @author Xiong Tang
 */
public class MappingDescriptionVisitor implements RouterFunctions.Visitor, RequestPredicates.Visitor {

	private final Deque<StringBuilder> predicateStack = new ArrayDeque<>();

	private final List<DispatcherHandlerMappingDescription> descriptions = new ArrayList<>();

	private final Stack<List<DispatcherHandlerMappingDescription>> nestedDescriptions = new Stack<>();

	public List<DispatcherHandlerMappingDescription> getDescriptions() {
		return this.descriptions;
	}

	// RouterFunctions.Visitor
	@Override
	public void startNested(RequestPredicate predicate) {
		this.predicateStack.push(new StringBuilder());
		this.nestedDescriptions.push(new ArrayList<>());
		predicate.accept(this);
	}

	@Override
	public void endNested(RequestPredicate predicate) {
		String predicateInfo = this.predicateStack.pop().toString();
		List<DispatcherHandlerMappingDescription> nested = this.nestedDescriptions.pop();
		DispatcherHandlerMappingDescription description = new DispatcherHandlerMappingDescription(predicateInfo, null, null, nested);
		saveDescription(description);
	}

	private void saveDescription(DispatcherHandlerMappingDescription description) {
		if (!this.nestedDescriptions.isEmpty()) {
			this.nestedDescriptions.peek().add(description);
		}
		else {
			this.descriptions.add(description);
		}
	}

	@Override
	public void route(RequestPredicate predicate, HandlerFunction<?> handlerFunction) {
		this.predicateStack.push(new StringBuilder());
		predicate.accept(this);
		String predicateInfo = this.predicateStack.pop().toString();
		DispatcherHandlerMappingDetails details = new DispatcherHandlerMappingDetails();
		details.setHandlerFunction(new HandlerFunctionDescription(handlerFunction));
		DispatcherHandlerMappingDescription description = new DispatcherHandlerMappingDescription(predicateInfo, handlerFunction.toString(), details);
		saveDescription(description);
	}

	@Override
	public void resources(Function<ServerRequest, Mono<Resource>> lookupFunction) {
		saveDescription(new DispatcherHandlerMappingDescription(null, lookupFunction.toString(), null));
	}


	@Override
	public void attributes(Map<String, Object> attributes) {
		// No action needed for attributes
	}

	@Override
	public void unknown(RouterFunction<?> routerFunction) {
		saveDescription(new DispatcherHandlerMappingDescription(null, routerFunction.toString(), null));
	}

	// RequestPredicates.Visitor
	@Override
	public void method(Set<HttpMethod> methods) {
		if (methods.size() == 1) {
			append(methods.iterator().next());
		}
		else {
			append(methods);
		}
	}

	private void append(Object value) {
		StringBuilder current = this.predicateStack.peek();
		if (current != null) {
			current.append(value);
		}
	}

	@Override
	public void path(String pattern) {
		append(pattern);
	}

	@Override
	public void pathExtension(String extension) {
		append(String.format("*.%s", extension));
	}

	@Override
	public void header(String name, String value) {
		append(String.format("%s: %s", name, value));
	}

	@Override
	public void queryParam(String name, String value) {
		append(String.format("?%s == %s", name, value));
	}

	@Override
	public void startAnd() {
		append('(');
	}

	@Override
	public void and() {
		append(" && ");
	}

	@Override
	public void endAnd() {
		append(')');
	}

	@Override
	public void startOr() {
		append('(');
	}

	@Override
	public void or() {
		append(" || ");
	}

	@Override
	public void endOr() {
		append(')');
	}

	@Override
	public void startNegate() {
		append("!(");
	}

	@Override
	public void endNegate() {
		append(')');
	}

	@Override
	public void unknown(RequestPredicate predicate) {
		append(predicate);
	}

}

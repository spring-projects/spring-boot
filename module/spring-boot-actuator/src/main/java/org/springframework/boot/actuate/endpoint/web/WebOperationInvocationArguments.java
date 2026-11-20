/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.web;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.actuate.endpoint.InvocationContext;

/**
 * Utility that can be used to get arguments for an {@link InvocationContext} from a web
 * request. Provides consistent resolution logic, regardless of the underlying technology.
 *
 * @author Phillip Webb
 * @since 4.0.9.1
 */
public final class WebOperationInvocationArguments {

	private WebOperationInvocationArguments() {
	}

	/**
	 * Get invocation arguments.
	 * @param webOperation the source web operation
	 * @param invocationArguments callback used to supply a builder with data
	 * @return the invocation arguments
	 */
	public static Map<String, Object> get(WebOperation webOperation, Consumer<Builder> invocationArguments) {
		Builder builder = new Builder(webOperation.getRequestPredicate());
		invocationArguments.accept(builder);
		return builder.build();
	}

	/**
	 * Builder used to create invocation arguments from the various parts of a web
	 * request.
	 */
	public static class Builder {

		private final WebOperationRequestPredicate requestPredicate;

		private @Nullable Map<String, ?> queryParameters;

		private @Nullable Map<String, ?> pathParameters;

		private @Nullable Map<String, String> body;

		private @Nullable Function<String, String[]> remainPathSegments;

		Builder(WebOperationRequestPredicate requestPredicate) {
			this.requestPredicate = requestPredicate;
		}

		/**
		 * Include elements from the request query parameters.
		 * @param queryParameters a map of query parameters (values may be a
		 * {@code String}, {@code String[]} or {@code List<String>}
		 */
		public void queryParameters(Map<String, ?> queryParameters) {
			this.queryParameters = queryParameters;
		}

		/**
		 * Include elements from the request path parameters.
		 * @param pathParameters a map of oath parameters (values may be a {@code String},
		 * {@code String[]} or {@code List<String>}
		 */
		public void pathParameters(Map<String, ?> pathParameters) {
			this.pathParameters = pathParameters;
		}

		/**
		 * Include elements from the request body.
		 * @param body the body
		 */
		public void body(@Nullable Map<String, String> body) {
			this.body = body;
		}

		/**
		 * Include the remaining path segments if needed.
		 * @param remainPathSegments a function to provide the segments given the
		 * {@code matchAllRemainingPathSegmentsVariable}
		 */
		public void remainPathSegments(Function<String, String[]> remainPathSegments) {
			this.remainPathSegments = remainPathSegments;
		}

		Map<String, Object> build() {
			Map<String, Object> arguments = new LinkedHashMap<>();
			arguments.putAll(flattenMap(this.queryParameters));
			arguments.putAll(flattenMap(this.pathParameters));
			if (this.body != null) {
				arguments.putAll(this.body);
			}
			String matchAllVariable = this.requestPredicate.getMatchAllRemainingPathSegmentsVariable();
			if (matchAllVariable != null) {
				arguments.remove(matchAllVariable);
				if (this.remainPathSegments != null) {
					arguments.put(matchAllVariable, this.remainPathSegments.apply(matchAllVariable));
				}
			}
			return Collections.unmodifiableMap(arguments);
		}

		private Map<String, Object> flattenMap(@Nullable Map<String, ?> map) {
			if (map == null) {
				return Collections.emptyMap();
			}
			Map<String, Object> result = new LinkedHashMap<>();
			map.forEach((name, values) -> result.put(name, flattenValue(values)));
			return result;
		}

		private Object flattenValue(Object value) {
			if (value instanceof String[] array) {
				return (array.length != 1) ? List.of(array) : array[0];
			}
			if (value instanceof List<?> list) {
				return (list.size() != 1) ? Collections.unmodifiableList(list) : list.get(0);
			}
			return value;
		}

	}

}

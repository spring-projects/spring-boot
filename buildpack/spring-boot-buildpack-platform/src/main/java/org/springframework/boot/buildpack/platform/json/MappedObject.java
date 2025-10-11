/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.buildpack.platform.json;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

/**
 * Base class for mapped JSON objects.
 *
 * @author Phillip Webb
 * @author Dmytro Nosan
 * @since 2.3.0
 */
public class MappedObject {

	private final JsonNode node;

	private final Lookup lookup;

	/**
	 * Create a new {@link MappedObject} instance.
	 * @param node the source node
	 * @param lookup method handle lookup
	 */
	protected MappedObject(JsonNode node, Lookup lookup) {
		this.node = node;
		this.lookup = lookup;
	}

	/**
	 * Return the source node of the mapped object.
	 * @return the source node
	 */
	protected final JsonNode getNode() {
		return this.node;
	}

	/**
	 * Get the value at the given JSON path expression as a specific type.
	 * @param <T> the data type
	 * @param expression the JSON path expression
	 * @param type the desired type. May be a simple JSON type or an interface
	 * @return the value
	 */
	protected <T> @Nullable T valueAt(String expression, Class<T> type) {
		return valueAt(this, this.node, this.lookup, expression, type);
	}

	/**
	 * Get a {@link Map} at the given JSON path expression with a value mapped from a
	 * related {@link JsonNode}.
	 * @param <V> the value type
	 * @param expression the JSON path expression
	 * @param valueMapper function to map the value from the {@link JsonNode}
	 * @return the map
	 * @since 3.5.0
	 */
	protected <V> Map<String, V> mapAt(String expression, Function<JsonNode, V> valueMapper) {
		Map<String, V> map = new LinkedHashMap<>();
		getNode().at(expression)
			.properties()
			.forEach((entry) -> map.put(entry.getKey(), valueMapper.apply(entry.getValue())));
		return Collections.unmodifiableMap(map);
	}

	/**
	 * Get children at the given JSON path expression by constructing them using the given
	 * factory.
	 * @param <T> the child type
	 * @param expression the JSON path expression
	 * @param factory factory used to create the child
	 * @return a list of children
	 * @since 3.2.6
	 */
	protected <T> List<T> childrenAt(@Nullable String expression, Function<JsonNode, T> factory) {
		JsonNode node = (expression != null) ? this.node.at(expression) : this.node;
		if (node.isEmpty()) {
			return Collections.emptyList();
		}
		List<T> children = new ArrayList<>();
		node.values().forEach((childNode) -> children.add(factory.apply(childNode)));
		return Collections.unmodifiableList(children);
	}

	@SuppressWarnings("unchecked")
	protected static <T extends MappedObject> T getRoot(Object proxy) {
		MappedInvocationHandler handler = (MappedInvocationHandler) Proxy.getInvocationHandler(proxy);
		return (T) handler.root;
	}

	protected static <T> @Nullable T valueAt(Object proxy, String expression, Class<T> type) {
		MappedInvocationHandler handler = (MappedInvocationHandler) Proxy.getInvocationHandler(proxy);
		return valueAt(handler.root, handler.node, handler.lookup, expression, type);
	}

	@SuppressWarnings("unchecked")
	private static <T> @Nullable T valueAt(MappedObject root, JsonNode node, Lookup lookup, String expression,
			Class<T> type) {
		JsonNode result = node.at(expression);
		if (result.isMissingNode() && expression.startsWith("/") && expression.length() > 1
				&& Character.isLowerCase(expression.charAt(1))) {
			StringBuilder alternative = new StringBuilder(expression);
			alternative.setCharAt(1, Character.toUpperCase(alternative.charAt(1)));
			result = node.at(alternative.toString());
		}
		if (type.isInterface() && !type.getName().startsWith("java")) {
			return (T) Proxy.newProxyInstance(MappedObject.class.getClassLoader(), new Class<?>[] { type },
					new MappedInvocationHandler(root, result, lookup));
		}
		if (result.isMissingNode()) {
			return null;
		}
		try {
			return SharedJsonMapper.get().treeToValue(result, type);
		}
		catch (JacksonException ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
	 * Factory method to create a new {@link MappedObject} instance.
	 * @param <T> the mapped object type
	 * @param content the JSON content for the object
	 * @param factory a factory to create the mapped object from a {@link JsonNode}
	 * @return the mapped object
	 * @throws IOException on IO error
	 */
	protected static <T extends MappedObject> T of(String content, Function<JsonNode, T> factory) throws IOException {
		return of(content, ObjectMapper::readTree, factory);
	}

	/**
	 * Factory method to create a new {@link MappedObject} instance.
	 * @param <T> the mapped object type
	 * @param content the JSON content for the object
	 * @param factory a factory to create the mapped object from a {@link JsonNode}
	 * @return the mapped object
	 * @throws IOException on IO error
	 */
	protected static <T extends MappedObject> T of(InputStream content, Function<JsonNode, T> factory)
			throws IOException {
		return of(StreamUtils.nonClosing(content), ObjectMapper::readTree, factory);
	}

	/**
	 * Factory method to create a new {@link MappedObject} instance.
	 * @param <T> the mapped object type
	 * @param <C> the content type
	 * @param content the JSON content for the object
	 * @param reader the content reader
	 * @param factory a factory to create the mapped object from a {@link JsonNode}
	 * @return the mapped object
	 * @throws IOException on IO error
	 */
	protected static <T extends MappedObject, C> T of(C content, ContentReader<C> reader, Function<JsonNode, T> factory)
			throws IOException {
		JsonMapper jsonMapper = SharedJsonMapper.get();
		JsonNode node = reader.read(jsonMapper, content);
		return factory.apply(node);
	}

	/**
	 * Strategy used to read JSON content.
	 *
	 * @param <C> the content type
	 */
	@FunctionalInterface
	protected interface ContentReader<C> {

		/**
		 * Read JSON content as a {@link JsonNode}.
		 * @param objectMapper the source object mapper
		 * @param content the content to read
		 * @return a {@link JsonNode}
		 * @throws IOException on IO error
		 */
		JsonNode read(ObjectMapper objectMapper, C content) throws IOException;

	}

	/**
	 * {@link InvocationHandler} used to support
	 * {@link MappedObject#valueAt(String, Class) valueAt} with {@code interface} types.
	 */
	private static class MappedInvocationHandler implements InvocationHandler {

		private static final String GET = "get";

		private static final String IS = "is";

		private final MappedObject root;

		private final JsonNode node;

		private final Lookup lookup;

		MappedInvocationHandler(MappedObject root, JsonNode node, Lookup lookup) {
			this.root = root;
			this.node = node;
			this.lookup = lookup;
		}

		@Override
		public @Nullable Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			Class<?> declaringClass = method.getDeclaringClass();
			if (method.isDefault()) {
				Lookup lookup = this.lookup.in(declaringClass);
				MethodHandle methodHandle = lookup.unreflectSpecial(method, declaringClass).bindTo(proxy);
				return methodHandle.invokeWithArguments();
			}
			if (declaringClass == Object.class) {
				method.invoke(proxy, args);
			}
			Assert.state(args == null || args.length == 0, () -> "Unsupported method " + method);
			String name = getName(method.getName());
			Class<?> type = method.getReturnType();
			return valueForProperty(name, type);
		}

		private String getName(String name) {
			StringBuilder result = new StringBuilder(name);
			if (name.startsWith(GET)) {
				result = new StringBuilder(name.substring(GET.length()));
			}
			if (name.startsWith(IS)) {
				result = new StringBuilder(name.substring(IS.length()));
			}
			Assert.state(result.length() >= 0, "Missing name");
			result.setCharAt(0, Character.toLowerCase(result.charAt(0)));
			return result.toString();
		}

		private @Nullable Object valueForProperty(String name, Class<?> type) {
			return valueAt(this.root, this.node, this.lookup, "/" + name, type);
		}

	}

}

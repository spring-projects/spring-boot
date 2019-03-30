/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.jackson;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

import org.springframework.util.Assert;

/**
 * Helper base class for {@link JsonDeserializer} implementations that deserialize
 * objects.
 *
 * @param <T> the supported object type
 * @author Phillip Webb
 * @since 1.4.0
 * @see JsonObjectSerializer
 */
public abstract class JsonObjectDeserializer<T>
		extends com.fasterxml.jackson.databind.JsonDeserializer<T> {

	@Override
	public final T deserialize(JsonParser jp, DeserializationContext ctxt)
			throws IOException {
		try {
			ObjectCodec codec = jp.getCodec();
			JsonNode tree = codec.readTree(jp);
			return deserializeObject(jp, ctxt, codec, tree);
		}
		catch (Exception ex) {
			if (ex instanceof IOException) {
				throw (IOException) ex;
			}
			throw new JsonMappingException(jp, "Object deserialize error", ex);
		}
	}

	/**
	 * Deserialize JSON content into the value type this serializer handles.
	 * @param jsonParser the source parser used for reading JSON content
	 * @param context context that can be used to access information about this
	 * deserialization activity
	 * @param codec the {@link ObjectCodec} associated with the parser
	 * @param tree deserialized JSON content as tree expressed using set of
	 * {@link TreeNode} instances
	 * @return the deserialized object
	 * @throws IOException on error
	 * @see #deserialize(JsonParser, DeserializationContext)
	 */
	protected abstract T deserializeObject(JsonParser jsonParser,
			DeserializationContext context, ObjectCodec codec, JsonNode tree)
			throws IOException;

	/**
	 * Helper method to extract a value from the given {@code jsonNode} or return
	 * {@code null} when the node itself is {@code null}.
	 * @param jsonNode the source node (may be {@code null})
	 * @param type the data type. May be {@link String}, {@link Boolean}, {@link Long},
	 * {@link Integer}, {@link Short}, {@link Double}, {@link Float}, {@link BigDecimal}
	 * or {@link BigInteger}.
	 * @param <D> the data type requested
	 * @return the node value or {@code null}
	 */
	@SuppressWarnings({ "unchecked" })
	protected final <D> D nullSafeValue(JsonNode jsonNode, Class<D> type) {
		Assert.notNull(type, "Type must not be null");
		if (jsonNode == null) {
			return null;
		}
		if (type == String.class) {
			return (D) jsonNode.textValue();
		}
		if (type == Boolean.class) {
			return (D) Boolean.valueOf(jsonNode.booleanValue());
		}
		if (type == Long.class) {
			return (D) Long.valueOf(jsonNode.longValue());
		}
		if (type == Integer.class) {
			return (D) Integer.valueOf(jsonNode.intValue());
		}
		if (type == Short.class) {
			return (D) Short.valueOf(jsonNode.shortValue());
		}
		if (type == Double.class) {
			return (D) Double.valueOf(jsonNode.doubleValue());
		}
		if (type == Float.class) {
			return (D) Float.valueOf(jsonNode.floatValue());
		}
		if (type == BigDecimal.class) {
			return (D) jsonNode.decimalValue();
		}
		if (type == BigInteger.class) {
			return (D) jsonNode.bigIntegerValue();
		}
		throw new IllegalArgumentException("Unsupported value type " + type.getName());
	}

	/**
	 * Helper method to return a {@link JsonNode} from the tree.
	 * @param tree the source tree
	 * @param fieldName the field name to extract
	 * @return the {@link JsonNode}
	 */
	protected final JsonNode getRequiredNode(JsonNode tree, String fieldName) {
		Assert.notNull(tree, "Tree must not be null");
		JsonNode node = tree.get(fieldName);
		Assert.state(node != null && !(node instanceof NullNode),
				() -> "Missing JSON field '" + fieldName + "'");
		return node;
	}

}

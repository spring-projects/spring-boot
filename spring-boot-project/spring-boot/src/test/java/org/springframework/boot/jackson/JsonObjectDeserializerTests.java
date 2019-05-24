/*
 * Copyright 2012-2019 the original author or authors.
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

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.NullNode;
import org.junit.jupiter.api.Test;

import org.springframework.boot.jackson.NameAndAgeJsonComponent.Deserializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JsonObjectDeserializer}.
 *
 * @author Phillip Webb
 */
class JsonObjectDeserializerTests {

	private TestJsonObjectDeserializer<Object> testDeserializer = new TestJsonObjectDeserializer<>();

	@Test
	void deserializeObjectShouldReadJson() throws Exception {
		Deserializer deserializer = new NameAndAgeJsonComponent.Deserializer();
		SimpleModule module = new SimpleModule();
		module.addDeserializer(NameAndAge.class, deserializer);
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(module);
		NameAndAge nameAndAge = mapper.readValue("{\"name\":\"spring\",\"age\":100}", NameAndAge.class);
		assertThat(nameAndAge.getName()).isEqualTo("spring");
		assertThat(nameAndAge.getAge()).isEqualTo(100);
	}

	@Test
	void nullSafeValueWhenValueIsNullShouldReturnNull() {
		String value = this.testDeserializer.testNullSafeValue(null, String.class);
		assertThat(value).isNull();
	}

	@Test
	void nullSafeValueWhenClassIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.testDeserializer.testNullSafeValue(mock(JsonNode.class), null))
				.withMessageContaining("Type must not be null");
	}

	@Test
	void nullSafeValueWhenClassIsStringShouldReturnString() {
		JsonNode node = mock(JsonNode.class);
		given(node.textValue()).willReturn("abc");
		String value = this.testDeserializer.testNullSafeValue(node, String.class);
		assertThat(value).isEqualTo("abc");
	}

	@Test
	void nullSafeValueWhenClassIsBooleanShouldReturnBoolean() {
		JsonNode node = mock(JsonNode.class);
		given(node.booleanValue()).willReturn(true);
		Boolean value = this.testDeserializer.testNullSafeValue(node, Boolean.class);
		assertThat(value).isTrue();
	}

	@Test
	void nullSafeValueWhenClassIsLongShouldReturnLong() {
		JsonNode node = mock(JsonNode.class);
		given(node.longValue()).willReturn(10L);
		Long value = this.testDeserializer.testNullSafeValue(node, Long.class);
		assertThat(value).isEqualTo(10L);
	}

	@Test
	void nullSafeValueWhenClassIsIntegerShouldReturnInteger() {
		JsonNode node = mock(JsonNode.class);
		given(node.intValue()).willReturn(10);
		Integer value = this.testDeserializer.testNullSafeValue(node, Integer.class);
		assertThat(value).isEqualTo(10);
	}

	@Test
	void nullSafeValueWhenClassIsShortShouldReturnShort() {
		JsonNode node = mock(JsonNode.class);
		given(node.shortValue()).willReturn((short) 10);
		Short value = this.testDeserializer.testNullSafeValue(node, Short.class);
		assertThat(value).isEqualTo((short) 10);
	}

	@Test
	void nullSafeValueWhenClassIsDoubleShouldReturnDouble() {
		JsonNode node = mock(JsonNode.class);
		given(node.doubleValue()).willReturn(1.1D);
		Double value = this.testDeserializer.testNullSafeValue(node, Double.class);
		assertThat(value).isEqualTo(1.1D);
	}

	@Test
	void nullSafeValueWhenClassIsFloatShouldReturnFloat() {
		JsonNode node = mock(JsonNode.class);
		given(node.floatValue()).willReturn(1.1F);
		Float value = this.testDeserializer.testNullSafeValue(node, Float.class);
		assertThat(value).isEqualTo(1.1F);
	}

	@Test
	void nullSafeValueWhenClassIsBigDecimalShouldReturnBigDecimal() {
		JsonNode node = mock(JsonNode.class);
		given(node.decimalValue()).willReturn(BigDecimal.TEN);
		BigDecimal value = this.testDeserializer.testNullSafeValue(node, BigDecimal.class);
		assertThat(value).isEqualTo(BigDecimal.TEN);
	}

	@Test
	void nullSafeValueWhenClassIsBigIntegerShouldReturnBigInteger() {
		JsonNode node = mock(JsonNode.class);
		given(node.bigIntegerValue()).willReturn(BigInteger.TEN);
		BigInteger value = this.testDeserializer.testNullSafeValue(node, BigInteger.class);
		assertThat(value).isEqualTo(BigInteger.TEN);
	}

	@Test
	void nullSafeValueWhenClassIsUnknownShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.testDeserializer.testNullSafeValue(mock(JsonNode.class), InputStream.class))
				.withMessageContaining("Unsupported value type java.io.InputStream");

	}

	@Test
	void getRequiredNodeWhenTreeIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.testDeserializer.testGetRequiredNode(null, "test"))
				.withMessageContaining("Tree must not be null");
	}

	@Test
	void getRequiredNodeWhenNodeIsNullShouldThrowException() {
		JsonNode tree = mock(JsonNode.class);
		given(tree.get("test")).willReturn(null);
		assertThatIllegalStateException().isThrownBy(() -> this.testDeserializer.testGetRequiredNode(tree, "test"))
				.withMessageContaining("Missing JSON field 'test'");
	}

	@Test
	void getRequiredNodeWhenNodeIsNullNodeShouldThrowException() {
		JsonNode tree = mock(JsonNode.class);
		given(tree.get("test")).willReturn(NullNode.instance);
		assertThatIllegalStateException().isThrownBy(() -> this.testDeserializer.testGetRequiredNode(tree, "test"))
				.withMessageContaining("Missing JSON field 'test'");
	}

	@Test
	void getRequiredNodeWhenNodeIsFoundShouldReturnNode() {
		JsonNode node = mock(JsonNode.class);
		JsonNode tree = node;
		given(tree.get("test")).willReturn(node);
		assertThat(this.testDeserializer.testGetRequiredNode(tree, "test")).isEqualTo(node);
	}

	static class TestJsonObjectDeserializer<T> extends JsonObjectDeserializer<T> {

		@Override
		protected T deserializeObject(JsonParser jsonParser, DeserializationContext context, ObjectCodec codec,
				JsonNode tree) {
			return null;
		}

		public <D> D testNullSafeValue(JsonNode jsonNode, Class<D> type) {
			return nullSafeValue(jsonNode, type);
		}

		public JsonNode testGetRequiredNode(JsonNode tree, String fieldName) {
			return getRequiredNode(tree, fieldName);
		}

	}

}

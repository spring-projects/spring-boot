/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.json;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.boot.json.JsonValueWriter.Series;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link JsonValueWriter} .
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 */
class JsonValueWriterTests {

	@Test
	void writeNameAndValueWhenNameIsNull() {
		assertThat(doWrite((writer) -> writer.write(null, "test"))).isEqualTo(quoted("test"));
	}

	@Test
	void writeNameAndValueWhenNameIsNotNull() {
		assertThat(doWrite((writer) -> {
			writer.start(Series.OBJECT);
			writer.write("name", "value");
			writer.end(Series.OBJECT);
		})).isEqualTo("""
				{"name":"value"}""");
	}

	@Test
	void writeWhenNull() {
		assertThat(write(null)).isEqualTo("null");
	}

	@Test
	void writeWhenWritableJson() {

		JsonWriter<String> writer = (instance, out) -> out.append("""
				{"test":"%s"}""".formatted(instance));
		assertThat(write(writer.write("hello"))).isEqualTo("""
				{"test":"hello"}""");
	}

	@Test
	void writeWhenStringArray() {
		assertThat(write(new String[] { "a", "b", "c" })).isEqualTo("""
				["a","b","c"]""");
	}

	@Test
	void writeWhenNumberArray() {
		assertThat(write(new int[] { 1, 2, 3 })).isEqualTo("[1,2,3]");
		assertThat(write(new double[] { 1.0, 2.0, 3.0 })).isEqualTo("[1.0,2.0,3.0]");
	}

	@Test
	void writeWhenBooleanArray() {
		assertThat(write(new boolean[] { true, false, true })).isEqualTo("[true,false,true]");
	}

	@Test
	void writeWhenArrayWithNullElements() {
		assertThat(write(new Object[] { null, null })).isEqualTo("[null,null]");
	}

	@Test
	void writeWhenArrayWithMixedElementTypes() {
		assertThat(write(new Object[] { "a", "b", "c", 1, 2, true, null })).isEqualTo("""
				["a","b","c",1,2,true,null]""");
	}

	@Test
	void writeWhenCollection() {
		assertThat(write(List.of("a", "b", "c"))).isEqualTo("""
				["a","b","c"]""");
		assertThat(write(new LinkedHashSet<>(List.of("a", "b", "c")))).isEqualTo("""
				["a","b","c"]""");
	}

	@Test
	void writeWhenMap() {
		Map<String, String> map = new LinkedHashMap<>();
		map.put("a", "A");
		map.put("b", "B");
		assertThat(write(map)).isEqualTo("""
				{"a":"A","b":"B"}""");
	}

	@Test
	void writeWhenMapWithNumericalKeys() {
		Map<Integer, String> map = new LinkedHashMap<>();
		map.put(1, "A");
		map.put(2, "B");
		assertThat(write(map)).isEqualTo("""
				{"1":"A","2":"B"}""");
	}

	@Test
	void writeWhenMapWithMixedValueTypes() {
		Map<Object, Object> map = new LinkedHashMap<>();
		map.put("a", 1);
		map.put("b", 2.0);
		map.put("c", true);
		map.put("d", "d");
		map.put("e", null);
		assertThat(write(map)).isEqualTo("""
				{"a":1,"b":2.0,"c":true,"d":"d","e":null}""");
	}

	@Test
	void writeWhenNumber() {
		assertThat(write((byte) 123)).isEqualTo("123");
		assertThat(write(123)).isEqualTo("123");
		assertThat(write(123L)).isEqualTo("123");
		assertThat(write(2.0)).isEqualTo("2.0");
		assertThat(write(2.0f)).isEqualTo("2.0");
		assertThat(write(Byte.valueOf((byte) 123))).isEqualTo("123");
		assertThat(write(Integer.valueOf(123))).isEqualTo("123");
		assertThat(write(Long.valueOf(123L))).isEqualTo("123");
		assertThat(write(Double.valueOf(2.0))).isEqualTo("2.0");
		assertThat(write(Float.valueOf(2.0f))).isEqualTo("2.0");
	}

	@Test
	void writeWhenBoolean() {
		assertThat(write(true)).isEqualTo("true");
		assertThat(write(Boolean.TRUE)).isEqualTo("true");
		assertThat(write(false)).isEqualTo("false");
		assertThat(write(Boolean.FALSE)).isEqualTo("false");
	}

	@Test
	void writeWhenString() {
		assertThat(write("test")).isEqualTo(quoted("test"));
	}

	@Test
	void writeWhenStringRequiringEscape() {
		assertThat(write("\"")).isEqualTo(quoted("\\\""));
		assertThat(write("\\")).isEqualTo(quoted("\\\\"));
		assertThat(write("/")).isEqualTo(quoted("\\/"));
		assertThat(write("\b")).isEqualTo(quoted("\\b"));
		assertThat(write("\f")).isEqualTo(quoted("\\f"));
		assertThat(write("\n")).isEqualTo(quoted("\\n"));
		assertThat(write("\r")).isEqualTo(quoted("\\r"));
		assertThat(write("\t")).isEqualTo(quoted("\\t"));
		assertThat(write("\u0000\u001F")).isEqualTo(quoted("\\u0000\\u001F"));
	}

	@Test
	void writeObject() {
		Map<String, String> map = Map.of("a", "A");
		String actual = doWrite((valueWriter) -> valueWriter.writeObject(map::forEach));
		assertThat(actual).isEqualTo("""
				{"a":"A"}""");
	}

	@Test
	void writePairs() {
		String actual = doWrite((valueWriter) -> {
			valueWriter.start(Series.OBJECT);
			valueWriter.writePairs(Map.of("a", "A")::forEach);
			valueWriter.writePairs(Map.of("b", "B")::forEach);
			valueWriter.end(Series.OBJECT);
		});
		assertThat(actual).isEqualTo("""
				{"a":"A","b":"B"}""");
	}

	@Test
	void writePairsWhenDuplicateThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> doWrite((valueWriter) -> {
			valueWriter.start(Series.OBJECT);
			valueWriter.writePairs(Map.of("a", "A")::forEach);
			valueWriter.writePairs(Map.of("a", "B")::forEach);
			valueWriter.end(Series.OBJECT);
		})).withMessage("The name 'a' has already been written");
	}

	@Test
	void writeArray() {
		List<String> list = List.of("a", "b", "c");
		String actual = doWrite((valueWriter) -> valueWriter.writeArray(list::forEach));
		assertThat(actual).isEqualTo("""
				["a","b","c"]""");
	}

	@Test
	void writeElements() {
		String actual = doWrite((valueWriter) -> {
			valueWriter.start(Series.ARRAY);
			valueWriter.writeElements(List.of("a", "b")::forEach);
			valueWriter.writeElements(List.of("c", "d")::forEach);
			valueWriter.end(Series.ARRAY);
		});
		assertThat(actual).isEqualTo("""
				["a","b","c","d"]""");
	}

	@Test
	void startAndEndWithNull() {
		String actual = doWrite((valueWriter) -> {
			valueWriter.start(null);
			valueWriter.write("test");
			valueWriter.end(null);
		});
		assertThat(actual).isEqualTo(quoted("test"));
	}

	@Test
	void endWhenNotStartedThrowsException() {
		doWrite((valueWriter) -> assertThatExceptionOfType(NoSuchElementException.class)
			.isThrownBy(() -> valueWriter.end(Series.ARRAY)));
	}

	private <V> String write(V value) {
		return doWrite((valueWriter) -> valueWriter.write(value));
	}

	private String doWrite(Consumer<JsonValueWriter> action) {
		StringBuilder out = new StringBuilder();
		action.accept(new JsonValueWriter(out));
		return out.toString();
	}

	private String quoted(String string) {
		return "\"" + string + "\"";
	}

}

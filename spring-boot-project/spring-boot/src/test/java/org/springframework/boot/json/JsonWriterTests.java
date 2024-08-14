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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.json.JsonWriter.PairExtractor;
import org.springframework.boot.json.JsonWriter.WritableJson;
import org.springframework.core.io.FileSystemResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link JsonWriter}.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 */
class JsonWriterTests {

	private static final Person PERSON = new Person("Spring", "Boot", 10);

	@TempDir
	File temp;

	@Test
	void writeToStringWritesToString() {
		assertThat(ofFormatString("%s").writeToString(123)).isEqualTo("123");
	}

	@Test
	void writeReturnsWritableJson() {
		assertThat(ofFormatString("%s").write(123)).isInstanceOf(WritableJson.class);
	}

	@Test
	void withSuffixAddsSuffixToWrittenString() {
		assertThat(ofFormatString("%s").withSuffix("000").writeToString(123)).isEqualTo("123000");
	}

	@Test
	void withSuffixWhenSuffixIsNullReturnsExistingWriter() {
		JsonWriter<?> writer = ofFormatString("%s");
		assertThat(writer.withSuffix(null)).isSameAs(writer);
	}

	@Test
	void withSuffixWhenSuffixIsEmptyReturnsExistingWriter() {
		JsonWriter<?> writer = ofFormatString("%s");
		assertThat(writer.withSuffix("")).isSameAs(writer);
	}

	@Test
	void withNewLineAtEndAddsNewLineToWrittenString() {
		assertThat(ofFormatString("%s").withNewLineAtEnd().writeToString(123)).isEqualTo("123\n");
	}

	@Test
	void ofAddingNamedSelf() {
		JsonWriter<Person> writer = JsonWriter.of((members) -> members.add("test"));
		assertThat(writer.writeToString(PERSON)).isEqualTo("""
				{"test":"Spring Boot (10)"}""");
	}

	@Test
	void ofAddingUnnamedSelf() {
		JsonWriter<Person> writer = JsonWriter.of((members) -> members.add());
		assertThat(writer.writeToString(PERSON)).isEqualTo(quoted("Spring Boot (10)"));
	}

	@Test
	void ofAddValue() {
		JsonWriter<Person> writer = JsonWriter.of((members) -> members.add("Spring", "Boot"));
		assertThat(writer.writeToString(PERSON)).isEqualTo("""
				{"Spring":"Boot"}""");
	}

	@Test
	void ofAddSupplier() {
		JsonWriter<Person> writer = JsonWriter.of((members) -> members.add("Spring", () -> "Boot"));
		assertThat(writer.writeToString(PERSON)).isEqualTo("""
				{"Spring":"Boot"}""");
	}

	@Test
	void ofAddExtractor() {
		JsonWriter<Person> writer = JsonWriter.of((members) -> {
			members.add("firstName", Person::firstName);
			members.add("lastName", Person::lastName);
			members.add("age", Person::age);
		});
		assertThat(writer.writeToString(PERSON)).isEqualTo("""
				{"firstName":"Spring","lastName":"Boot","age":10}""");
	}

	@Test
	void ofFromValue() {
		JsonWriter<Person> writer = JsonWriter.of((members) -> members.from("Boot"));
		assertThat(writer.writeToString(PERSON)).isEqualTo(quoted("Boot"));
	}

	@Test
	void ofFromSupplier() {
		JsonWriter<Person> writer = JsonWriter.of((members) -> members.from(() -> "Boot"));
		assertThat(writer.writeToString(PERSON)).isEqualTo(quoted("Boot"));
	}

	@Test
	void ofFromExtractor() {
		JsonWriter<Person> writer = JsonWriter.of((members) -> members.from(Person::lastName));
		assertThat(writer.writeToString(PERSON)).isEqualTo(quoted("Boot"));
	}

	@Test
	void ofAddMapEntries() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("a", "A");
		map.put("b", 123);
		map.put("c", true);
		JsonWriter<List<Map<String, Object>>> writer = JsonWriter
			.of((members) -> members.addMapEntries((list) -> list.get(0)));
		assertThat(writer.writeToString(List.of(map))).isEqualTo("""
				{"a":"A","b":123,"c":true}""");
	}

	@Test
	void ofWhenNoMembersAddedThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> JsonWriter.of((members) -> {
		})).withMessage("No members have been added");
	}

	@Test
	void ofWhenOneContributesPairByNameAndOneHasNoNameThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> JsonWriter.of((members) -> {
			members.add("Spring", "Boot");
			members.from("alone");
		}))
			.withMessage("Member at index 1 does not contribute a named pair, "
					+ "ensure that all members have a name or call an appropriate 'using' method");
	}

	@Test
	void ofWhenOneContributesPairByUsingPairsAndOneHasNoNameThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> JsonWriter.of((members) -> {
			members.from(Map.of("Spring", "Boot")).usingPairs(Map::forEach);
			members.from("alone");
		}))
			.withMessage("Member at index 1 does not contribute a named pair, "
					+ "ensure that all members have a name or call an appropriate 'using' method");
	}

	@Test
	void ofWhenOneContributesPairByUsingMembersAndOneHasNoNameThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> JsonWriter.of((members) -> {
			members.from(PERSON).usingMembers((personMembers) -> {
				personMembers.add("first", Person::firstName);
				personMembers.add("last", Person::firstName);
			});
			members.from("alone");
		}))
			.withMessage("Member at index 1 does not contribute a named pair, "
					+ "ensure that all members have a name or call an appropriate 'using' method");
	}

	private static String quoted(String value) {
		return "\"" + value + "\"";
	}

	private static <T> JsonWriter<T> ofFormatString(String json) {
		return (instance, out) -> out.append(json.formatted(instance));
	}

	@Nested
	class StandardWriterTests {

		@Test
		void whenPrimitive() {
			assertThat(write(null)).isEqualTo("null");
			assertThat(write(123)).isEqualTo("123");
			assertThat(write(true)).isEqualTo("true");
			assertThat(write("test")).isEqualTo(quoted("test"));
		}

		@Test
		void whenMap() {
			assertThat(write(Map.of("spring", "boot"))).isEqualTo("""
					{"spring":"boot"}""");
		}

		@Test
		void whenArray() {
			assertThat(write(new int[] { 1, 2, 3 })).isEqualTo("[1,2,3]");
		}

		private <T> String write(T instance) {
			return JsonWriter.standard().writeToString(instance);
		}

	}

	@Nested
	class MemberTest {

		@Test
		void whenNotNull() {
			JsonWriter<String> writer = JsonWriter.of((members) -> members.add().whenNotNull());
			assertThat(writer.writeToString("test")).isEqualTo(quoted("test"));
			assertThat(writer.writeToString(null)).isEmpty();
		}

		@Test
		void whenNotNullExtracted() {
			Person personWithNull = new Person("Spring", null, 10);
			JsonWriter<Person> writer = JsonWriter.of((members) -> members.add().whenNotNull(Person::lastName));
			assertThat(writer.writeToString(PERSON)).isEqualTo(quoted("Spring Boot (10)"));
			assertThat(writer.writeToString(personWithNull)).isEmpty();
		}

		@Test
		void whenHasLength() {
			JsonWriter<String> writer = JsonWriter.of((members) -> members.add().whenHasLength());
			assertThat(writer.writeToString("test")).isEqualTo(quoted("test"));
			assertThat(writer.writeToString("")).isEmpty();
			assertThat(writer.writeToString(null)).isEmpty();
		}

		@Test
		void whenHasLengthOnNonString() {
			JsonWriter<StringBuilder> writer = JsonWriter.of((members) -> members.add().whenHasLength());
			assertThat(writer.writeToString(new StringBuilder("test"))).isEqualTo(quoted("test"));
			assertThat(writer.writeToString(new StringBuilder())).isEmpty();
			assertThat(writer.writeToString(null)).isEmpty();
		}

		@Test
		void whenNotEmpty() {
			JsonWriter<Object> writer = JsonWriter.of((members) -> members.add().whenNotEmpty());
			assertThat(writer.writeToString(List.of("a"))).isEqualTo("""
					["a"]""");
			assertThat(writer.writeToString(Collections.emptyList())).isEmpty();
			assertThat(writer.writeToString(new Object[] {})).isEmpty();
			assertThat(writer.writeToString(new int[] {})).isEmpty();
			assertThat(writer.writeToString(null)).isEmpty();
		}

		@Test
		void whenNot() {
			JsonWriter<List<String>> writer = JsonWriter.of((members) -> members.add().whenNot(List::isEmpty));
			assertThat(writer.writeToString(List.of("a"))).isEqualTo("""
					["a"]""");
			assertThat(writer.writeToString(Collections.emptyList())).isEmpty();
		}

		@Test
		void when() {
			JsonWriter<List<String>> writer = JsonWriter.of((members) -> members.add().when(List::isEmpty));
			assertThat(writer.writeToString(List.of("a"))).isEmpty();
			assertThat(writer.writeToString(Collections.emptyList())).isEqualTo("[]");
		}

		@Test
		void chainedPredicates() {
			Set<String> banned = Set.of("Spring", "Boot");
			JsonWriter<String> writer = JsonWriter.of((members) -> members.add()
				.whenHasLength()
				.whenNot(banned::contains)
				.whenNot((string) -> string.length() <= 2));
			assertThat(writer.writeToString("")).isEmpty();
			assertThat(writer.writeToString("a")).isEmpty();
			assertThat(writer.writeToString("Boot")).isEmpty();
			assertThat(writer.writeToString("JSON")).isEqualTo(quoted("JSON"));
		}

		@Test
		void as() {
			JsonWriter<String> writer = JsonWriter.of((members) -> members.add().as(Integer::valueOf));
			assertThat(writer.writeToString("123")).isEqualTo("123");
		}

		@Test
		void asWhenValueIsNullDoesNotCallAdapter() {
			JsonWriter<String> writer = JsonWriter.of((members) -> members.add().as((value) -> {
				throw new RuntimeException("bad");
			}));
			writer.writeToString(null);
		}

		@Test
		void chainedAs() {
			Function<Integer, Boolean> booleanAdapter = (integer) -> integer != 0;
			JsonWriter<String> writer = JsonWriter
				.of((members) -> members.add().as(Integer::valueOf).as(booleanAdapter));
			assertThat(writer.writeToString("0")).isEqualTo("false");
			assertThat(writer.writeToString("1")).isEqualTo("true");
		}

		@Test
		void chainedAsAndPredicates() {
			Function<Integer, Boolean> booleanAdapter = (integer) -> integer != 0;
			JsonWriter<String> writer = JsonWriter.of((members) -> members.add()
				.whenNot(String::isEmpty)
				.as(Integer::valueOf)
				.when((integer) -> integer < 2)
				.as(booleanAdapter));
			assertThat(writer.writeToString("")).isEmpty();
			assertThat(writer.writeToString("0")).isEqualTo("false");
			assertThat(writer.writeToString("1")).isEqualTo("true");
			assertThat(writer.writeToString("2")).isEmpty();
		}

		@Test
		void usingExtractedPairsWithExtractor() {
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("a", "A");
			map.put("b", "B");
			PairExtractor<Map.Entry<String, Object>> extractor = PairExtractor.of(Map.Entry::getKey,
					Map.Entry::getValue);
			JsonWriter<Map<String, Object>> writer = JsonWriter
				.of((members) -> members.add().as(Map::entrySet).usingExtractedPairs(Set::forEach, extractor));
			assertThat(writer.writeToString(map)).isEqualTo("""
					{"a":"A","b":"B"}""");
		}

		@Test
		void usingExtractedPairs() {
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("a", "A");
			map.put("b", "B");
			Function<Map.Entry<String, Object>, String> nameExtractor = Map.Entry::getKey;
			Function<Map.Entry<String, Object>, Object> valueExtractor = Map.Entry::getValue;
			JsonWriter<Map<String, Object>> writer = JsonWriter.of((members) -> members.add()
				.as(Map::entrySet)
				.usingExtractedPairs(Set::forEach, nameExtractor, valueExtractor));
			assertThat(writer.writeToString(map)).isEqualTo("""
					{"a":"A","b":"B"}""");
		}

		@Test
		void usingPairs() {
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("a", "A");
			map.put("b", "B");
			JsonWriter<Map<String, Object>> writer = JsonWriter.of((members) -> members.add().usingPairs(Map::forEach));
			assertThat(writer.writeToString(map)).isEqualTo("""
					{"a":"A","b":"B"}""");
		}

		@Test
		void usingPairsWhenAlreadyDeclaredThrowsException() {
			assertThatIllegalStateException().isThrownBy(() -> JsonWriter.of((
					members) -> members.from(Collections.emptyMap()).usingPairs(Map::forEach).usingPairs(Map::forEach)))
				.withMessage("Pairs cannot be declared multiple times");
		}

		@Test
		void usingPairsWhenUsingMembersThrowsException() {
			assertThatIllegalStateException()
				.isThrownBy(() -> JsonWriter.of((members) -> members.from(Collections.emptyMap())
					.usingMembers((mapMembers) -> mapMembers.add("test"))
					.usingPairs(Map::forEach)))
				.withMessage("Pairs cannot be declared when using members");
		}

		@Test
		void usingMembers() {
			Couple couple = new Couple(PERSON, new Person("Spring", "Framework", 20));
			JsonWriter<Couple> writer = JsonWriter.of((member) -> {
				member.add("personOne", Couple::person1).usingMembers((personMembers) -> {
					personMembers.add("fn", Person::firstName);
					personMembers.add("ln", Person::lastName);
				});
				member.add("personTwo", Couple::person2).usingMembers((personMembers) -> {
					personMembers.add("details", Person::toString);
					personMembers.add("eldest", true);
				});
			});
			assertThat(writer.writeToString(couple)).isEqualTo("""
					{"personOne":{"fn":"Spring","ln":"Boot"},""" + """
					"personTwo":{"details":"Spring Framework (20)","eldest":true}}""");
		}

		@Test
		void usingMembersWithoutName() {
			Couple couple = new Couple(PERSON, new Person("Spring", "Framework", 20));
			JsonWriter<Couple> writer = JsonWriter.of((member) -> {
				member.add("version", 1);
				member.from(Couple::person1)
					.usingMembers((personMembers) -> personMembers.add("one", Person::toString));
				member.from(Couple::person2)
					.usingMembers((personMembers) -> personMembers.add("two", Person::toString));
			});
			assertThat(writer.writeToString(couple)).isEqualTo("""
					{"version":1,"one":"Spring Boot (10)","two":"Spring Framework (20)"}""");
		}

		@Test
		void usingMembersWithoutNameInMember() {
			Couple couple = new Couple(PERSON, new Person("Spring", "Framework", 20));
			JsonWriter<Couple> writer = JsonWriter.of((member) -> member.add("only", Couple::person2)
				.usingMembers((personMembers) -> personMembers.from(Person::toString)));
			assertThat(writer.writeToString(couple)).isEqualTo("""
					{"only":"Spring Framework (20)"}""");
		}

		@Test
		void usingMemebersWithoutNameAtAll() {
			Couple couple = new Couple(PERSON, new Person("Spring", "Framework", 20));
			JsonWriter<Couple> writer = JsonWriter.of((member) -> member.from(Couple::person2)
				.usingMembers((personMembers) -> personMembers.from(Person::toString)));
			assertThat(writer.writeToString(couple)).isEqualTo(quoted("Spring Framework (20)"));
		}

		@Test
		void usingMembersWhenAlreadyDeclaredThrowsException() {
			assertThatIllegalStateException()
				.isThrownBy(() -> JsonWriter.of((members) -> members.from(Collections.emptyMap())
					.usingMembers((mapMembers) -> mapMembers.add("test"))
					.usingMembers((mapMembers) -> mapMembers.add("test"))))
				.withMessage("Members cannot be declared multiple times");
		}

		@Test
		void usingMembersWhenUsingPairsThrowsException() {
			assertThatIllegalStateException()
				.isThrownBy(() -> JsonWriter.of((members) -> members.from(Collections.emptyMap())
					.usingPairs(Map::forEach)
					.usingMembers((mapMembers) -> mapMembers.add("test"))))
				.withMessage("Members cannot be declared when using pairs");
		}

	}

	@Nested
	class WritableJsonTests {

		@Test
		void toJsonStringReturnsString() {
			WritableJson writable = (out) -> out.append("{}");
			assertThat(writable.toJsonString()).isEqualTo("{}");
		}

		@Test
		void toJsonStringWhenIOExceptionIsThrownThrowsUncheckedIOException() {
			WritableJson writable = (out) -> {
				throw new IOException("bad");
			};
			assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> writable.toJsonString())
				.havingCause()
				.withMessage("bad");
		}

		@Test
		void toByteArrayReturnsByteArray() {
			WritableJson writable = (out) -> out.append("{}");
			assertThat(writable.toByteArray()).isEqualTo("{}".getBytes());
		}

		@Test
		void toResourceWritesJson() throws Exception {
			File file = new File(JsonWriterTests.this.temp, "out.json");
			WritableJson writable = (out) -> out.append("{}");
			writable.toResource(new FileSystemResource(file));
			assertThat(file).content().isEqualTo("{}");
		}

		@Test
		void toResourceWithCharsetWritesJson() throws Exception {
			File file = new File(JsonWriterTests.this.temp, "out.json");
			WritableJson writable = (out) -> out.append("{}");
			writable.toResource(new FileSystemResource(file), StandardCharsets.ISO_8859_1);
			assertThat(file).content(StandardCharsets.ISO_8859_1).isEqualTo("{}");
		}

		@Test
		void toResourceWithCharsetWhenOutIsNullThrowsException() {
			WritableJson writable = (out) -> out.append("{}");
			assertThatIllegalArgumentException().isThrownBy(() -> writable.toResource(null, StandardCharsets.UTF_8))
				.withMessage("'out' must not be null");
		}

		@Test
		void toResourceWithCharsetWhenCharsetIsNullThrowsException() {
			File file = new File(JsonWriterTests.this.temp, "out.json");
			WritableJson writable = (out) -> out.append("{}");
			assertThatIllegalArgumentException()
				.isThrownBy(() -> writable.toResource(new FileSystemResource(file), null))
				.withMessage("'charset' must not be null");
		}

		@Test
		void toOutputStreamWritesJson() throws Exception {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			WritableJson writable = (out) -> out.append("{}");
			writable.toOutputStream(outputStream);
			assertThat(outputStream.toString(StandardCharsets.UTF_8)).isEqualTo("{}");
		}

		@Test
		void toOutputStreamWithCharsetWritesJson() throws Exception {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			WritableJson writable = (out) -> out.append("{}");
			writable.toOutputStream(outputStream, StandardCharsets.ISO_8859_1);
			assertThat(outputStream.toString(StandardCharsets.ISO_8859_1)).isEqualTo("{}");
		}

		@Test
		void toOutputStreamWithCharsetWhenOutIsNullThrowsException() {
			WritableJson writable = (out) -> out.append("{}");
			assertThatIllegalArgumentException().isThrownBy(() -> writable.toOutputStream(null, StandardCharsets.UTF_8))
				.withMessage("'out' must not be null");
		}

		@Test
		void toOutputStreamWithCharsetWhenCharsetIsNullThrowsException() {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			WritableJson writable = (out) -> out.append("{}");
			assertThatIllegalArgumentException().isThrownBy(() -> writable.toOutputStream(outputStream, null))
				.withMessage("'charset' must not be null");
		}

		//

		@Test
		void toWriterWritesJson() throws Exception {
			StringWriter writer = new StringWriter();
			WritableJson writable = (out) -> out.append("{}");
			writable.toWriter(writer);
			assertThat(writer).hasToString("{}");
		}

		@Test
		void toWriterWhenWriterIsNullThrowsException() {
			WritableJson writable = (out) -> out.append("{}");
			assertThatIllegalArgumentException().isThrownBy(() -> writable.toWriter(null))
				.withMessage("'out' must not be null");
		}

		@Test
		void ofReturnsInstanceWithSensibleToString() {
			WritableJson writable = WritableJson.of((out) -> out.append("{}"));
			assertThat(writable).hasToString("{}");
		}

	}

	record Person(String firstName, String lastName, int age) {

		@Override
		public String toString() {
			return "%s %s (%s)".formatted(this.firstName, this.lastName, this.age);
		}

	}

	record Couple(Person person1, Person person2) {

	}

}

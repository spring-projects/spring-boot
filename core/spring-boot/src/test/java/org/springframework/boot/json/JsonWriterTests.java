/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.boot.json.JsonWriter.Extractor;
import org.springframework.boot.json.JsonWriter.Member;
import org.springframework.boot.json.JsonWriter.MemberPath;
import org.springframework.boot.json.JsonWriter.Members;
import org.springframework.boot.json.JsonWriter.NameProcessor;
import org.springframework.boot.json.JsonWriter.PairExtractor;
import org.springframework.boot.json.JsonWriter.ValueProcessor;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Tests for {@link JsonWriter}.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 * @author Diego Magalhães
 */
class JsonWriterTests {

	private static final Person PERSON = new Person("Spring", "Boot", 10);

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
		Supplier<@Nullable String> supplier = () -> "Boot";
		JsonWriter<Person> writer = JsonWriter.of((members) -> members.add("Spring", supplier));
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
		Supplier<@Nullable String> supplier = () -> "Boot";
		JsonWriter<Person> writer = JsonWriter.of((members) -> members.from(supplier));
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

	/**
	 * Tests for {@link JsonWriter#standard()}.
	 */
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

		private <T> String write(@Nullable T instance) {
			return JsonWriter.standard().writeToString(instance);
		}

	}

	/**
	 * Tests for {@link Members} and {@link Member}.
	 */
	@Nested
	class MembersTest {

		@Test
		void whenNotNull() {
			JsonWriter<String> writer = JsonWriter.of((members) -> members.add().whenNotNull());
			assertThat(writer.writeToString("test")).isEqualTo(quoted("test"));
			assertThat(writer.writeToString(null)).isEmpty();
		}

		@Test
		void whenNotNullExtracted() {
			Person personWithNull = new Person("Spring", null, 10);
			Function<@Nullable Person, @Nullable Object> lastName = (person) -> (person != null) ? person.lastName()
					: null;
			JsonWriter<Person> writer = JsonWriter.of((members) -> members.add().whenNotNull(lastName));
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
			Predicate<@Nullable List<String>> isEmpty = List::isEmpty;
			JsonWriter<List<String>> writer = JsonWriter.of((members) -> members.add().whenNot(isEmpty));
			assertThat(writer.writeToString(List.of("a"))).isEqualTo("""
					["a"]""");
			assertThat(writer.writeToString(Collections.emptyList())).isEmpty();
		}

		@Test
		void when() {
			Predicate<@Nullable List<String>> isEmpty = List::isEmpty;
			JsonWriter<List<String>> writer = JsonWriter.of((members) -> members.add().when(isEmpty));
			assertThat(writer.writeToString(List.of("a"))).isEmpty();
			assertThat(writer.writeToString(Collections.emptyList())).isEqualTo("[]");
		}

		@Test
		void chainedPredicates() {
			Set<String> banned = Set.of("Spring", "Boot");
			Predicate<@Nullable String> stringLengthPredicate = (string) -> string != null && string.length() <= 2;
			Predicate<@Nullable String> bannedPredicate = (string) -> string != null && banned.contains(string);
			JsonWriter<String> writer = JsonWriter
				.of((members) -> members.add().whenHasLength().whenNot(bannedPredicate).whenNot(stringLengthPredicate));
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
			Extractor<Integer, Boolean> booleanExtractor = (integer) -> integer != 0;
			JsonWriter<String> writer = JsonWriter
				.of((members) -> members.add().as(Integer::valueOf).as(booleanExtractor));
			assertThat(writer.writeToString("0")).isEqualTo("false");
			assertThat(writer.writeToString("1")).isEqualTo("true");
		}

		@Test
		void chainedAsAndPredicates() {
			Extractor<Integer, Boolean> booleanExtractor = (integer) -> integer != 0;
			Predicate<@Nullable String> isEmpty = (string) -> !StringUtils.hasLength(string);
			JsonWriter<String> writer = JsonWriter.of((members) -> members.add()
				.whenNot(isEmpty)
				.as(Integer::valueOf)
				.when((integer) -> integer < 2)
				.as(booleanExtractor));
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
			JsonWriter<Couple> writer = JsonWriter.of((members) -> {
				members.add("personOne", Couple::person1).usingMembers((personMembers) -> {
					personMembers.add("fn", Person::firstName);
					personMembers.add("ln", Person::lastName);
				});
				members.add("personTwo", Couple::person2).usingMembers((personMembers) -> {
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
			JsonWriter<Couple> writer = JsonWriter.of((members) -> {
				members.add("version", 1);
				members.from(Couple::person1)
					.usingMembers((personMembers) -> personMembers.add("one", Person::toString));
				members.from(Couple::person2)
					.usingMembers((personMembers) -> personMembers.add("two", Person::toString));
			});
			assertThat(writer.writeToString(couple)).isEqualTo("""
					{"version":1,"one":"Spring Boot (10)","two":"Spring Framework (20)"}""");
		}

		@Test
		void usingMembersWithoutNameInMember() {
			Couple couple = new Couple(PERSON, new Person("Spring", "Framework", 20));
			JsonWriter<Couple> writer = JsonWriter.of((members) -> members.add("only", Couple::person2)
				.usingMembers((personMembers) -> personMembers.from(Person::toString)));
			assertThat(writer.writeToString(couple)).isEqualTo("""
					{"only":"Spring Framework (20)"}""");
		}

		@Test
		void usingMembersWithoutNameAtAll() {
			Couple couple = new Couple(PERSON, new Person("Spring", "Framework", 20));
			JsonWriter<Couple> writer = JsonWriter.of((members) -> members.from(Couple::person2)
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

	/**
	 * Tests for {@link MemberPath}.
	 */
	@Nested
	class MemberPathTests {

		@Test
		void createWhenIndexAndNamedThrowException() {
			assertThatIllegalArgumentException().isThrownBy(() -> new MemberPath(null, "boot", 0))
				.withMessage("'name' and 'index' cannot be mixed");
			assertThatIllegalArgumentException().isThrownBy(() -> new MemberPath(null, null, -1))
				.withMessage("'name' and 'index' cannot be mixed");
		}

		@Test
		void toStringReturnsUsefulString() {
			assertThat(MemberPath.ROOT).hasToString("");
			MemberPath spring = new MemberPath(MemberPath.ROOT, "spring", MemberPath.UNINDEXED);
			MemberPath springDotBoot = new MemberPath(spring, "boot", MemberPath.UNINDEXED);
			MemberPath springZero = new MemberPath(spring, null, 0);
			MemberPath springZeroDotBoot = new MemberPath(springZero, "boot", MemberPath.UNINDEXED);
			assertThat(spring).hasToString("spring");
			assertThat(springDotBoot).hasToString("spring.boot");
			assertThat(springZero).hasToString("spring[0]");
			assertThat(springZeroDotBoot).hasToString("spring[0].boot");
		}

		@Test
		void childWithNameCreatesChild() {
			assertThat(MemberPath.ROOT.child("spring").child("boot")).hasToString("spring.boot");
		}

		@Test
		void childWithNameWhenNameSpecialChars() {
			assertThat(MemberPath.ROOT.child("spring.io").child("boot")).hasToString("spring\\.io.boot");
			assertThat(MemberPath.ROOT.child("spring[io]").child("boot")).hasToString("spring\\[io\\].boot");
			assertThat(MemberPath.ROOT.child("spring.[io]").child("boot")).hasToString("spring\\.\\[io\\].boot");
			assertThat(MemberPath.ROOT.child("spring\\io").child("boot")).hasToString("spring\\\\io.boot");
			assertThat(MemberPath.ROOT.child("spring.\\io").child("boot")).hasToString("spring\\.\\\\io.boot");
			assertThat(MemberPath.ROOT.child("spring[\\io]").child("boot")).hasToString("spring\\[\\\\io\\].boot");
			assertThat(MemberPath.ROOT.child("123").child("boot")).hasToString("123.boot");
			assertThat(MemberPath.ROOT.child("1.2.3").child("boot")).hasToString("1\\.2\\.3.boot");
		}

		@Test
		void childWithIndexCreatesChild() {
			assertThat(MemberPath.ROOT.child("spring").child(0)).hasToString("spring[0]");
		}

		@Test
		void ofParsesPaths() {
			assertOfFromToString(MemberPath.ROOT.child("spring").child("boot"));
			assertOfFromToString(MemberPath.ROOT.child("spring").child(0));
			assertOfFromToString(MemberPath.ROOT.child("spring.io").child("boot"));
			assertOfFromToString(MemberPath.ROOT.child("spring[io]").child("boot"));
			assertOfFromToString(MemberPath.ROOT.child("spring.[io]").child("boot"));
			assertOfFromToString(MemberPath.ROOT.child("spring\\io").child("boot"));
			assertOfFromToString(MemberPath.ROOT.child("spring.\\io").child("boot"));
			assertOfFromToString(MemberPath.ROOT.child("spring[\\io]").child("boot"));
			assertOfFromToString(MemberPath.ROOT.child("123").child("boot"));
			assertOfFromToString(MemberPath.ROOT.child("1.2.3").child("boot"));
		}

		private void assertOfFromToString(MemberPath path) {
			assertThat(MemberPath.of(path.toString())).isEqualTo(path);
		}

	}

	/**
	 * Tests for {@link Members#applyingPathFilter(java.util.function.Predicate)}.
	 */
	@Nested
	class PathFilterTests {

		@Test
		void filteringMember() {
			JsonWriter<Person> writer = JsonWriter.of((members) -> {
				members.add("first", Person::firstName);
				members.add("last", Person::lastName);
				members.applyingPathFilter((path) -> {
					String name = path.name();
					assertThat(name).isNotNull();
					return name.equals("first");
				});
			});
			assertThat(writer.writeToString(new Person("spring", "boot", 10))).isEqualTo("""
					{"last":"boot"}""");
		}

		@Test
		void filteringInMap() {
			JsonWriter<Map<?, ?>> writer = JsonWriter.of((members) -> {
				members.add();
				members.applyingPathFilter((path) -> {
					String name = path.name();
					assertThat(name).isNotNull();
					return name.equals("spring");
				});

			});
			assertThat(writer.writeToString(Map.of("spring", "boot", "test", "test"))).isEqualTo("""
					{"test":"test"}""");
		}

	}

	/**
	 * Tests for {@link NameProcessor}.
	 */
	@Nested
	class NameProcessorTests {

		@Test
		void processNameWhenSimpleValue() {
			JsonWriter<String> writer = JsonWriter.of((members) -> {
				members.add();
				members.applyingNameProcessor(NameProcessor.of(String::toUpperCase));
			});
			assertThat(writer.writeToString("test")).isEqualTo("\"test\"");
		}

		@Test
		void processNameWhenMember() {
			JsonWriter<Person> writer = JsonWriter.of((members) -> {
				members.add("first", Person::firstName);
				members.add("last", Person::lastName);
				members.applyingNameProcessor(NameProcessor.of(String::toUpperCase));
			});
			assertThat(writer.writeToString(new Person("spring", "boot", 10))).isEqualTo("""
					{"FIRST":"spring","LAST":"boot"}""");
		}

		@Test
		void processNameWhenInMap() {
			JsonWriter<Map<?, ?>> writer = JsonWriter.of((members) -> {
				members.add();
				members.applyingNameProcessor(NameProcessor.of(String::toUpperCase));
			});
			assertThat(writer.writeToString(Map.of("spring", "boot"))).isEqualTo("""
					{"SPRING":"boot"}""");
		}

		@Test
		void processNameWhenInNestedMap() {
			JsonWriter<Map<?, ?>> writer = JsonWriter.of((members) -> {
				members.add();
				members.applyingNameProcessor(NameProcessor.of(String::toUpperCase));
			});
			assertThat(writer.writeToString(Map.of("test", Map.of("spring", "boot")))).isEqualTo("""
					{"TEST":{"SPRING":"boot"}}""");
		}

		@Test
		void processNameWhenInPairs() {
			JsonWriter<Map<?, ?>> writer = JsonWriter.of((members) -> {
				members.add().usingPairs(Map::forEach);
				members.applyingNameProcessor(NameProcessor.of(String::toUpperCase));
			});
			assertThat(writer.writeToString(Map.of("spring", "boot"))).isEqualTo("""
					{"SPRING":"boot"}""");
		}

		@Test
		void processNameWhenHasNestedMembers() {
			Couple couple = new Couple(PERSON, new Person("Spring", "Framework", 20));
			JsonWriter<Couple> writer = JsonWriter.of((members) -> {
				members.from(Couple::person1)
					.usingMembers((personMembers) -> personMembers.add("one", Person::toString));
				members.from(Couple::person2)
					.usingMembers((personMembers) -> personMembers.add("two", Person::toString));
				members.applyingNameProcessor(NameProcessor.of(String::toUpperCase));
			});
			assertThat(writer.writeToString(couple)).isEqualTo("""
					{"ONE":"Spring Boot (10)","TWO":"Spring Framework (20)"}""");
		}

		@Test
		void processNameWhenHasNestedMembersWithAdditionalValueProcessor() {
			Couple couple = new Couple(PERSON, new Person("Spring", "Framework", 20));
			JsonWriter<Couple> writer = JsonWriter.of((members) -> {
				members.from(Couple::person1)
					.usingMembers((personMembers) -> personMembers.add("one", Person::toString));
				members.from(Couple::person2).usingMembers((personMembers) -> {
					personMembers.add("two", Person::toString);
					personMembers.applyingNameProcessor(NameProcessor.of(String::toUpperCase));
				});
				members.applyingNameProcessor(NameProcessor.of((name) -> name + "!"));
			});
			assertThat(writer.writeToString(couple)).isEqualTo("""
					{"one!":"Spring Boot (10)","TWO!":"Spring Framework (20)"}""");
		}

		@Test
		void processNameWhenDeeplyNestedUsesCompoundPaths() {
			List<String> paths = new ArrayList<>();
			JsonWriter<Couple> writer = JsonWriter.of((members) -> {
				members.add("one", Couple::person1).usingMembers((personMembers) -> {
					personMembers.add("first", Person::firstName);
					personMembers.add("last", Person::lastName);
				});
				members.add("two", Couple::person2).usingMembers((personMembers) -> {
					personMembers.add("first", Person::firstName);
					personMembers.add("last", Person::lastName);
				});
				members.applyingNameProcessor((path, existingName) -> {
					paths.add(path.toString());
					return existingName;
				});
			});
			Couple couple = new Couple(PERSON, new Person("Spring", "Framework", 20));
			writer.writeToString(couple);
			assertThat(paths).containsExactly("one", "one.first", "one.last", "two", "two.first", "two.last");
		}

		@Test
		void processNameWhenReturnsNullThrowsException() {
			JsonWriter<Person> writer = JsonWriter.of((members) -> {
				members.add("first", Person::firstName);
				members.add("last", Person::lastName);
				members
					.applyingNameProcessor((path, existingName) -> !"first".equals(existingName) ? existingName : null);
			});
			assertThatIllegalStateException().isThrownBy(() -> writer.writeToString(new Person("spring", "boot", 10)))
				.withMessageContaining("NameProcessor")
				.withMessageContaining("returned an empty result");
		}

		@Test
		void processNameWhenReturnsEmptyStringThrowsException() {
			JsonWriter<Person> writer = JsonWriter.of((members) -> {
				members.add("first", Person::firstName);
				members.add("last", Person::lastName);
				members
					.applyingNameProcessor((path, existingName) -> !"first".equals(existingName) ? existingName : "");
			});
			assertThatIllegalStateException().isThrownBy(() -> writer.writeToString(new Person("spring", "boot", 10)))
				.withMessageContaining("NameProcessor")
				.withMessageContaining("returned an empty result");
		}

	}

	/**
	 * Tests for {@link ValueProcessor}.
	 */
	@Nested
	class ValueProcessorTests {

		@Test
		void of() {
			ValueProcessor<String> processor = ValueProcessor.of(stringToUppercase());
			assertThat(processor.processValue(MemberPath.ROOT, "test")).isEqualTo("TEST");
		}

		@Test
		@SuppressWarnings("NullAway") // Test null check
		void ofWhenNull() {
			assertThatIllegalArgumentException().isThrownBy(() -> ValueProcessor.of(null))
				.withMessage("'action' must not be null");
		}

		@Test
		void whenHasPathWithStringWhenPathMatches() {
			ValueProcessor<String> processor = ValueProcessor.of(stringToUppercase()).whenHasPath("foo");
			assertThat(processor.processValue(MemberPath.ROOT.child("foo"), "test")).isEqualTo("TEST");
		}

		@Test
		void whenHasPathWithStringWhenPathDoesNotMatch() {
			ValueProcessor<String> processor = ValueProcessor.<String>of(stringToUppercase()).whenHasPath("foo");
			assertThat(processor.processValue(MemberPath.ROOT.child("bar"), "test")).isEqualTo("test");
		}

		@Test
		void whenHasPathWithPredicateWhenPathMatches() {
			ValueProcessor<String> processor = ValueProcessor.<String>of(stringToUppercase())
				.whenHasPath((path) -> path.toString().startsWith("f"));
			assertThat(processor.processValue(MemberPath.ROOT.child("foo"), "test")).isEqualTo("TEST");
		}

		@Test
		void whenHasPathWithPredicateWhenPathDoesNotMatch() {
			ValueProcessor<String> processor = ValueProcessor.<String>of(stringToUppercase())
				.whenHasPath((path) -> path.toString().startsWith("f"));
			assertThat(processor.processValue(MemberPath.ROOT.child("bar"), "test")).isEqualTo("test");
		}

		@Test
		void whenInstanceOfWhenInstanceMatches() {
			ValueProcessor<Object> processor = ValueProcessor.of(objectToUppercase()).whenInstanceOf(String.class);
			assertThat(processor.processValue(MemberPath.ROOT, "test")).hasToString("TEST");
		}

		@Test
		void whenInstanceOfWhenInstanceDoesNotMatch() {
			ValueProcessor<Object> processor = ValueProcessor.of(objectToUppercase()).whenInstanceOf(String.class);
			assertThat(processor.processValue(MemberPath.ROOT, new StringBuilder("test"))).hasToString("test");
		}

		@Test
		void whenWhenPredicateMatches() {
			Predicate<@Nullable String> equals = "test"::equals;
			ValueProcessor<String> processor = ValueProcessor.of(stringToUppercase()).when(equals);
			assertThat(processor.processValue(MemberPath.ROOT, "test")).isEqualTo("TEST");
		}

		@Test
		void whenWhenPredicateDoesNotMatch() {
			Predicate<@Nullable String> equals = "test"::equals;
			ValueProcessor<String> processor = ValueProcessor.of(stringToUppercase()).when(equals);
			assertThat(processor.processValue(MemberPath.ROOT, "other")).isEqualTo("other");
		}

		@Test
		void processValueWhenSimpleValue() {
			JsonWriter<String> writer = simpleWriterWithUppercaseProcessor();
			assertThat(writer.writeToString("test")).isEqualTo("\"TEST\"");
		}

		@Test
		void processValueWhenMemberValue() {
			JsonWriter<Person> writer = JsonWriter.of((members) -> {
				members.add("first", Person::firstName);
				members.add("last", Person::lastName);
				members.applyingValueProcessor(ValueProcessor.of(stringCapitalize()));
			});
			assertThat(writer.writeToString(new Person("spring", "boot", 10))).isEqualTo("""
					{"first":"Spring","last":"Boot"}""");
		}

		@Test
		void processValueWhenInMap() {
			JsonWriter<Map<?, ?>> writer = JsonWriter.of((members) -> {
				members.add();
				members.applyingValueProcessor(ValueProcessor.of(stringCapitalize()));
			});
			assertThat(writer.writeToString(Map.of("spring", "boot"))).isEqualTo("""
					{"spring":"Boot"}""");
		}

		@Test
		void processValueWhenInNestedMap() {
			JsonWriter<Map<?, ?>> writer = JsonWriter.of((members) -> {
				members.add();
				members.applyingValueProcessor(ValueProcessor.of(stringCapitalize()));
			});
			assertThat(writer.writeToString(Map.of("test", Map.of("spring", "boot")))).isEqualTo("""
					{"test":{"spring":"Boot"}}""");
		}

		@Test
		void processValueWhenInPairs() {
			JsonWriter<Map<?, ?>> writer = JsonWriter.of((members) -> {
				members.add().usingPairs(Map::forEach);
				members.applyingValueProcessor(ValueProcessor.of(stringCapitalize()));
			});
			assertThat(writer.writeToString(Map.of("spring", "boot"))).isEqualTo("""
					{"spring":"Boot"}""");
		}

		@Test
		void processValueWhenCalledWithMultipleTypesIgnoresLambdaErrors() {
			JsonWriter<Object> writer = JsonWriter.of((members) -> {
				members.add();
				members.applyingValueProcessor(ValueProcessor.of(stringCapitalize()));
			});
			assertThat(writer.writeToString("spring")).isEqualTo("\"Spring\"");
			assertThat(writer.writeToString(123)).isEqualTo("123");
			assertThat(writer.writeToString(true)).isEqualTo("true");
		}

		@Test
		void processValueWhenLimitedToPath() {
			JsonWriter<Map<?, ?>> writer = JsonWriter.of((members) -> {
				members.add();
				members.applyingValueProcessor(ValueProcessor.of(stringCapitalize()).whenHasPath("spring"));
			});
			assertThat(writer.writeToString(Map.of("spring", "boot"))).isEqualTo("""
					{"spring":"Boot"}""");
			assertThat(writer.writeToString(Map.of("boot", "spring"))).isEqualTo("""
					{"boot":"spring"}""");
		}

		@Test
		void processValueWhen() {
			JsonWriter<Map<?, ?>> writer = JsonWriter.of((members) -> {
				members.add();
				Predicate<@Nullable String> startsWithB = (candidate) -> candidate != null && candidate.startsWith("b");
				members.applyingValueProcessor(ValueProcessor.of(stringCapitalize()).when(startsWithB));
			});
			assertThat(writer.writeToString(Map.of("spring", "boot"))).isEqualTo("""
					{"spring":"Boot"}""");
			assertThat(writer.writeToString(Map.of("boot", "spring"))).isEqualTo("""
					{"boot":"spring"}""");
		}

		@Test
		void processValueWhenHasNestedMembers() {
			Couple couple = new Couple(PERSON, new Person("Spring", "Framework", 20));
			JsonWriter<Couple> writer = JsonWriter.of((members) -> {
				members.from(Couple::person1)
					.usingMembers((personMembers) -> personMembers.add("one", Person::toString));
				members.from(Couple::person2)
					.usingMembers((personMembers) -> personMembers.add("two", Person::toString));
				UnaryOperator<@Nullable String> toUpperCase = (string) -> (string != null)
						? string.toUpperCase(Locale.ROOT) : null;
				members.applyingValueProcessor(ValueProcessor.of(String.class, toUpperCase));
			});
			assertThat(writer.writeToString(couple)).isEqualTo("""
					{"one":"SPRING BOOT (10)","two":"SPRING FRAMEWORK (20)"}""");
		}

		@Test
		void processValueWhenHasNestedMembersWithAdditionalValueProcessor() {
			Couple couple = new Couple(PERSON, new Person("Spring", "Framework", 20));
			JsonWriter<Couple> writer = JsonWriter.of((members) -> {
				members.from(Couple::person1)
					.usingMembers((personMembers) -> personMembers.add("one", Person::toString));
				members.from(Couple::person2).usingMembers((personMembers) -> {
					personMembers.add("two", Person::toString);
					UnaryOperator<@Nullable String> operator = (item) -> item + "!";
					personMembers.applyingValueProcessor(ValueProcessor.of(String.class, operator));
				});
				UnaryOperator<@Nullable String> toUpperCase = (string) -> (string != null)
						? string.toUpperCase(Locale.ROOT) : null;
				members.applyingValueProcessor(ValueProcessor.of(String.class, toUpperCase));
			});
			assertThat(writer.writeToString(couple)).isEqualTo("""
					{"one":"SPRING BOOT (10)","two":"SPRING FRAMEWORK (20)!"}""");
		}

		@Test
		void processValueWhenDeeplyNestedUsesCompoundPaths() {
			List<String> paths = new ArrayList<>();
			JsonWriter<Couple> writer = JsonWriter.of((members) -> {
				members.add("one", Couple::person1).usingMembers((personMembers) -> {
					personMembers.add("first", Person::firstName);
					personMembers.add("last", Person::lastName);
				});
				members.add("two", Couple::person2).usingMembers((personMembers) -> {
					personMembers.add("first", Person::firstName);
					personMembers.add("last", Person::lastName);
				});
				members.applyingValueProcessor((path, value) -> {
					paths.add(path.toString());
					return value;
				});
			});
			Couple couple = new Couple(PERSON, new Person("Spring", "Framework", 20));
			writer.writeToString(couple);
			assertThat(paths).containsExactly("one", "one.first", "one.last", "two", "two.first", "two.last");
		}

		@Test
		void processValueWhenUsingListUsesIndexedPaths() {
			List<String> paths = new ArrayList<>();
			JsonWriter<List<String>> writer = JsonWriter.of((members) -> {
				members.add();
				members.applyingValueProcessor((path, value) -> {
					paths.add(path.toString());
					return value;
				});
			});
			writer.writeToString(List.of("a", "b", "c"));
			assertThat(paths).containsExactly("", "[0]", "[1]", "[2]");
		}

		@Test
		void processValueUsesUnprocessedNameInPath() {
			List<String> paths = new ArrayList<>();
			JsonWriter<Person> writer = JsonWriter.of((members) -> {
				members.add("first", Person::firstName);
				members.add("last", Person::lastName);
				members.applyingValueProcessor((path, value) -> {
					paths.add(path.toString());
					return value;
				});
				members.applyingNameProcessor((path, existingName) -> "the-" + existingName);
			});
			writer.writeToString(PERSON);
			assertThat(paths).containsExactly("first", "last");
		}

		private static UnaryOperator<@Nullable String> stringToUppercase() {
			return (string) -> (string != null) ? string.toUpperCase(Locale.ROOT) : null;
		}

		private static UnaryOperator<@Nullable Object> objectToUppercase() {
			return (string) -> (string != null) ? string.toString().toUpperCase(Locale.ROOT) : null;
		}

		private static UnaryOperator<@Nullable String> stringCapitalize() {
			return (string) -> (string != null) ? StringUtils.capitalize(string) : null;
		}

		private JsonWriter<String> simpleWriterWithUppercaseProcessor() {
			return JsonWriter.of((members) -> {
				members.add();
				UnaryOperator<@Nullable String> toUpperCase = (string) -> (string != null)
						? string.toUpperCase(Locale.ROOT) : null;
				members.applyingValueProcessor(ValueProcessor.of(String.class, toUpperCase));
			});
		}

	}

	/**
	 * Tests for string escaping.
	 */
	@Nested
	class StringEscapingTests {

		private static final List<Charset> CHARSETS = List.of(StandardCharsets.UTF_8, StandardCharsets.ISO_8859_1,
				StandardCharsets.US_ASCII, StandardCharsets.UTF_16);

		private final JsonWriter<Object> writer = JsonWriter.standard();

		@Test
		void writeWhenEmptyString() {
			assertThat(this.writer.writeToString("")).isEqualTo("\"\"");
			assertThat(this.writer.write("").toByteArray(StandardCharsets.UTF_16))
				.isEqualTo(bytes("FE FF 00 22 00 22"));
		}

		@Test
		void writeEscapesEveryIsoControlCharacter() {
			List<String> values = isoControlValues();
			assertThat(values).hasSize(67);
			for (String value : values) {
				assertThat(this.writer.writeToString(value)).isEqualTo(expectedJson(value));
			}
			assertThat(this.writer.writeToString("\u0000\u001F\u007F\u0085\u009F"))
				.isEqualTo("\"\\u0000\\u001F\\u007F\\u0085\\u009F\"")
				.hasSize(32);
		}

		@Test
		void writeDoesNotEscapeOtherCharacters() {
			StringBuilder printable = new StringBuilder();
			for (char ch = 0x20; ch < 0x7F; ch++) {
				if (ch != '"' && ch != '\\') {
					printable.append(ch);
				}
			}
			List<String> values = List.of(printable.toString(), "/", "\u2028", "\u2029", "\u00A0", "\u00AD", "\uFEFF",
					"\uFFFD");
			for (String value : values) {
				assertThat(this.writer.writeToString(value)).isEqualTo("\"" + value + "\"");
			}
			WritableJson json = this.writer.write("\u2028\u2029/");
			assertThat(json.toByteArray(StandardCharsets.UTF_8)).isEqualTo(bytes("22 E2 80 A8 E2 80 A9 2F 22"));
			assertThat(json.toByteArray(StandardCharsets.US_ASCII)).isEqualTo(bytes("22 3F 3F 2F 22"));
		}

		@Test
		void writeEscapesQuoteAndBackslash() {
			for (String value : List.of("\"", "\\", "\"\"", "\\\"", "a\"b\\c", "\"abc", "abc\"")) {
				assertThat(this.writer.writeToString(value)).isEqualTo(expectedJson(value));
			}
			assertThat(this.writer.writeToString("a\"b\\c")).isEqualTo("\"a\\\"b\\\\c\"");
		}

		@Test
		void writeWhenEscapesAtStartEndAndAdjacent() {
			for (String value : List.of("\nabc", "abc\n", "\n\n\t\r", "a\n\nb", "\u0001\u0002")) {
				assertThat(this.writer.writeToString(value)).isEqualTo(expectedJson(value));
			}
			assertThat(this.writer.writeToString("\n\n\t\r")).isEqualTo("\"\\n\\n\\t\\r\"");
		}

		@Test
		void writeWhenLongStrings() {
			List<String> values = new ArrayList<>();
			values.add("a".repeat(1_048_576));
			values.add("a\n".repeat(524_288));
			values.add("\"".repeat(100_000));
			for (int length : new int[] { 8191, 8192, 8193, 16385 }) {
				values.add(withEscapeAt(length, 8191));
				values.add(withEscapeAt(length, length - 1));
			}
			for (String value : values) {
				String expected = expectedJson(value);
				assertThat(this.writer.writeToString(value)).isEqualTo(expected);
				assertThat(this.writer.write(value).toByteArray(StandardCharsets.UTF_8))
					.isEqualTo(encode(expected, StandardCharsets.UTF_8));
			}
		}

		@Test
		void writeWhenSurrogates() {
			for (String value : surrogateValues()) {
				WritableJson json = this.writer.write(value);
				assertThat(json.toJsonString()).isEqualTo(expectedJson(value)).doesNotContain("\\u");
				for (Charset charset : CHARSETS) {
					assertThat(json.toByteArray(charset)).isEqualTo(encode(json.toJsonString(), charset));
				}
			}
			assertThat(this.writer.write("\uD83D\"").toByteArray(StandardCharsets.UTF_8))
				.isEqualTo(bytes("22 3F 5C 22 22"));
			assertThat(this.writer.write("\uD83D\"").toByteArray(StandardCharsets.UTF_16))
				.isEqualTo(bytes("FE FF 00 22 FF FD 00 5C 00 22 00 22"));
			assertThat(this.writer.write("a\uD83D").toByteArray(StandardCharsets.UTF_8))
				.isEqualTo(bytes("22 61 3F 22"));
			assertThat(this.writer.write("\uDE00b").toByteArray(StandardCharsets.UTF_8))
				.isEqualTo(bytes("22 3F 62 22"));
			WritableJson emoji = this.writer.write("\uD83D\uDE00");
			assertThat(emoji.toByteArray(StandardCharsets.UTF_8)).isEqualTo(bytes("22 F0 9F 98 80 22"));
			assertThat(emoji.toByteArray(StandardCharsets.US_ASCII)).isEqualTo(bytes("22 3F 22"));
			assertThat(emoji.toByteArray(StandardCharsets.ISO_8859_1)).isEqualTo(bytes("22 3F 22"));
			assertThat(emoji.toByteArray(StandardCharsets.UTF_16)).isEqualTo(bytes("FE FF 00 22 D8 3D DE 00 00 22"));
		}

		@Test
		void writeToByteArrayMatchesEncodedJsonString() {
			for (String value : randomValues()) {
				for (Object shape : shapes(value)) {
					WritableJson json = this.writer.write(shape);
					String jsonString = json.toJsonString();
					for (Charset charset : CHARSETS) {
						assertThat(json.toByteArray(charset)).isEqualTo(encode(jsonString, charset));
					}
				}
			}
		}

		@Test
		void writeNamesRequiringEscape() {
			String name = "a\"b\n\u0001";
			assertThat(this.writer.writeToString(Map.of(name, 1))).isEqualTo("{" + expectedJson(name) + ":1}")
				.isEqualTo("{\"a\\\"b\\n\\u0001\":1}");
			JsonWriter<Object> writer = JsonWriter.of((members) -> members.add("x\ty", "v"));
			assertThat(writer.writeToString("ignored")).isEqualTo("{\"x\\ty\":\"v\"}");
		}

		@Test
		void writeWhenValueIsNotAString() {
			assertThat(this.writer.writeToString(new StringBuilder("a\"b"))).isEqualTo("\"a\\\"b\"");
			assertThat(this.writer.writeToString(new ToStringValue("q\"\n"))).isEqualTo("\"q\\\"\\n\"");
			assertThat(this.writer.writeToString(Double.NaN)).isEqualTo("\"NaN\"");
		}

		@Test
		void writeWhenToStringReturnsNullThrowsNullPointerException() {
			ToStringValue value = new ToStringValue(null);
			assertThatNullPointerException().isThrownBy(() -> this.writer.writeToString(value));
			assertThatNullPointerException()
				.isThrownBy(() -> this.writer.write(value).toByteArray(StandardCharsets.UTF_8));
		}

		@Test
		void writeWhenToStringThrowsPropagatesException() {
			Object value = new Object() {

				@Override
				public String toString() {
					throw new IllegalStateException("boom");
				}

			};
			assertThatIllegalStateException().isThrownBy(() -> this.writer.writeToString(value)).withMessage("boom");
			assertThatIllegalStateException()
				.isThrownBy(() -> this.writer.write(value).toByteArray(StandardCharsets.UTF_8))
				.withMessage("boom");
		}

		@Test
		void writeToWriterAndOutputStreamMatchesToJsonString() throws IOException {
			for (Object shape : surrogateAndRandomShapes()) {
				WritableJson json = this.writer.write(shape);
				String jsonString = json.toJsonString();
				StringWriter stringWriter = new StringWriter();
				json.toWriter(stringWriter);
				assertThat(stringWriter).hasToString(jsonString);
				for (Charset charset : CHARSETS) {
					ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
					json.toOutputStream(outputStream, charset);
					assertThat(outputStream.toByteArray()).isEqualTo(encode(jsonString, charset));
				}
			}
		}

		@Test
		void writeToAppendableReceivesSameContent() throws IOException {
			for (Object shape : surrogateAndRandomShapes()) {
				WritableJson json = this.writer.write(shape);
				StringBuilder content = new StringBuilder();
				json.to(forwarding(content));
				assertThat(content).hasToString(json.toJsonString());
			}
		}

		@Test
		void writeToFixedCapacityCharBufferThrowsBufferOverflowException() {
			assertThatExceptionOfType(BufferOverflowException.class)
				.isThrownBy(() -> this.writer.write("abcdef").to(CharBuffer.allocate(4)));
		}

		@Test
		void writeWhenDefaultLocaleIsNotRoot() {
			List<String> values = isoControlValues();
			List<String> expected = values.stream().map(this::expectedJson).toList();
			Locale defaultLocale = Locale.getDefault();
			Locale displayLocale = Locale.getDefault(Locale.Category.DISPLAY);
			Locale formatLocale = Locale.getDefault(Locale.Category.FORMAT);
			try {
				for (String tag : List.of("tr-TR", "th-TH-u-nu-thai", "ar-SA-u-nu-arab", "hi-IN-u-nu-deva")) {
					Locale.setDefault(Locale.forLanguageTag(tag));
					for (int i = 0; i < values.size(); i++) {
						assertThat(this.writer.writeToString(values.get(i))).as(tag).isEqualTo(expected.get(i));
					}
				}
			}
			finally {
				Locale.setDefault(defaultLocale);
				Locale.setDefault(Locale.Category.DISPLAY, displayLocale);
				Locale.setDefault(Locale.Category.FORMAT, formatLocale);
			}
		}

		@Test
		void writeToByteArrayWhenCalledRepeatedlyOnSameThread() {
			List<String> values = List.of("a".repeat(100_000), "x", "a\uD83D", "\uDE00b");
			for (Charset charset : List.of(StandardCharsets.UTF_8, StandardCharsets.UTF_16, StandardCharsets.UTF_8)) {
				for (String value : values) {
					assertThat(this.writer.write(value).toByteArray(charset)).as(charset.name())
						.isEqualTo(encode(expectedJson(value), charset));
				}
			}
			this.writer.write("a\uD83D").toByteArray(StandardCharsets.UTF_8);
			assertThat(this.writer.write("\uDE00b").toByteArray(StandardCharsets.UTF_8))
				.isEqualTo(bytes("22 3F 62 22"));
		}

		@Test
		void writeToByteArrayWhenValueToStringReentersToByteArray() {
			// Characterizes current behavior of a re-entrant call on the same thread
			// (latent bug, reported separately); not a contract
			AtomicReference<byte[]> inner = new AtomicReference<>();
			Object value = new Object() {

				@Override
				public String toString() {
					inner.set(JsonWriter.standard().write("inner").toByteArray(StandardCharsets.UTF_8));
					return "X";
				}

			};
			byte[] outer = JsonWriter.standard().write(value).toByteArray(StandardCharsets.UTF_8);
			assertThat(inner.get()).isEqualTo("\"\"inner\"".getBytes(StandardCharsets.UTF_8));
			assertThat(outer).isEqualTo("X\"".getBytes(StandardCharsets.UTF_8));
		}

		@Test
		void writeWhenSurrogatesStraddleBufferBoundaries() throws IOException {
			List<String> values = boundaryValues();
			assertThat(values).hasSize(470);
			for (int i = 0; i < values.size(); i++) {
				String value = values.get(i);
				String expected = expectedJson(value);
				WritableJson json = this.writer.write(value);
				assertThat(this.writer.writeToString(value)).as("writeToString %s", i).isEqualTo(expected);
				assertThat(json.toJsonString()).as("toJsonString %s", i).isEqualTo(expected);
				StringWriter stringWriter = new StringWriter();
				json.toWriter(stringWriter);
				assertThat(stringWriter).as("toWriter %s", i).hasToString(expected);
				StringBuilder forwarded = new StringBuilder();
				json.to(forwarding(forwarded));
				assertThat(forwarded).as("to(Appendable) %s", i).hasToString(expected);
				for (Charset charset : CHARSETS) {
					byte[] expectedBytes = encode(expected, charset);
					assertThat(json.toByteArray(charset)).as("toByteArray %s %s", charset, i).isEqualTo(expectedBytes);
					ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
					json.toOutputStream(outputStream, charset);
					assertThat(outputStream.toByteArray()).as("toOutputStream %s %s", charset, i)
						.isEqualTo(expectedBytes);
				}
			}
		}

		@Test
		void writeWhenAppendableThrowsIOExceptionThrowsUncheckedIOException() {
			assertThatExceptionOfType(UncheckedIOException.class)
				.isThrownBy(() -> this.writer.write("abc").to(throwingAppendable(true)))
				.havingCause()
				.isInstanceOf(IOException.class)
				.withMessage("x");
			assertThatExceptionOfType(UncheckedIOException.class)
				.isThrownBy(() -> this.writer.write("a\nb").to(throwingAppendable(false)))
				.havingCause()
				.isInstanceOf(IOException.class)
				.withMessage("x");
		}

		private List<String> isoControlValues() {
			List<String> values = new ArrayList<>();
			StringBuilder all = new StringBuilder();
			StringBuilder interleaved = new StringBuilder("a");
			for (char ch = 0; ch <= 0x9F; ch++) {
				if (Character.isISOControl(ch)) {
					values.add(String.valueOf(ch));
					all.append(ch);
					interleaved.append(ch).append('a');
				}
			}
			assertThat(all).hasSize(65);
			values.add(all.toString());
			values.add(interleaved.toString());
			return values;
		}

		private String withEscapeAt(int length, int index) {
			char[] chars = new char[length];
			Arrays.fill(chars, 'a');
			if (index < length) {
				chars[index] = (index == length - 1) ? '"' : '\n';
			}
			return new String(chars);
		}

		private List<String> surrogateValues() {
			return List.of("\uD83D\uDE00", "\u4E2D\u6587", "a\uD83D", "\uD83D\"", "\uD83D\n", "\uDE00b",
					"\uDE00\uD83D");
		}

		private List<String> randomValues() {
			Random random = new Random(4711);
			List<String> values = new ArrayList<>();
			for (int i = 0; i < 2000; i++) {
				int length = (i % 100 == 99) ? 9000 + random.nextInt(11001) : random.nextInt(601);
				StringBuilder value = new StringBuilder(length + 1);
				while (value.length() < length) {
					appendRandomCharacter(value, random);
				}
				values.add(value.toString());
			}
			return values;
		}

		private void appendRandomCharacter(StringBuilder value, Random random) {
			int kind = random.nextInt(100);
			if (kind < 50) {
				value.append((char) (0x20 + random.nextInt(0x5F)));
			}
			else if (kind < 58) {
				value.append((char) random.nextInt(0x20));
			}
			else if (kind < 60) {
				value.append((char) (0x7F + random.nextInt(0x21)));
			}
			else if (kind < 66) {
				value.append("\"\\/".charAt(random.nextInt(3)));
			}
			else if (kind < 68) {
				value.append((char) (0x2028 + random.nextInt(2)));
			}
			else if (kind < 78) {
				value.append((char) ((random.nextBoolean() ? 0x00C0 : 0x4E00) + random.nextInt(0x100)));
			}
			else if (kind < 88) {
				value.appendCodePoint(0x10000 + random.nextInt(0x100000));
			}
			else if (kind < 94) {
				value.append((char) (0xD800 + random.nextInt(0x400)));
			}
			else {
				value.append((char) (0xDC00 + random.nextInt(0x400)));
			}
		}

		private List<Object> shapes(String value) {
			return List.of(value, Map.of(value, 1), List.of(value, Map.of("k", value)));
		}

		private List<Object> surrogateAndRandomShapes() {
			List<Object> shapes = new ArrayList<>();
			surrogateValues().forEach((value) -> shapes.addAll(shapes(value)));
			randomValues().forEach((value) -> shapes.addAll(shapes(value)));
			return shapes;
		}

		private List<String> boundaryValues() {
			List<String> values = new ArrayList<>();
			for (int boundary = 2048; boundary <= 65536; boundary *= 2) {
				for (int offset = -6; offset <= 0; offset++) {
					for (String prefix : List.of("", "\n")) {
						String run = prefix + "x".repeat(boundary + offset);
						values.add(run + "\uD83D\uDE00" + "x".repeat(10));
						values.add(run + "\uD83D\"");
						values.add(run + "\uD83D\n");
						values.add(run + "\uD83D");
						values.add(run + "\uDE00" + "x".repeat(10));
					}
				}
			}
			SplittableRandom random = new SplittableRandom(2026);
			for (int i = 0; i < 50; i++) {
				int length = 8000 + random.nextInt(62001);
				StringBuilder value = new StringBuilder(length + 1);
				while (value.length() < length) {
					int kind = random.nextInt(100);
					if (kind < 50) {
						value.append((char) ('a' + random.nextInt(26)));
					}
					else if (kind < 75) {
						value.append((char) (0x4E00 + random.nextInt(0x5000)));
					}
					else if (kind < 85) {
						value.append((char) (0xA0 + random.nextInt(0x60)));
					}
					else {
						value.appendCodePoint(0x1F300 + random.nextInt(0x300));
					}
				}
				values.add(value.toString());
			}
			return values;
		}

		private String expectedJson(String value) {
			StringBuilder expected = new StringBuilder("\"");
			for (int i = 0; i < value.length(); i++) {
				char ch = value.charAt(i);
				switch (ch) {
					case '"' -> expected.append("\\\"");
					case '\\' -> expected.append("\\\\");
					case '\b' -> expected.append("\\b");
					case '\f' -> expected.append("\\f");
					case '\n' -> expected.append("\\n");
					case '\r' -> expected.append("\\r");
					case '\t' -> expected.append("\\t");
					default -> expected
						.append(Character.isISOControl(ch) ? String.format(Locale.ROOT, "\\u%04X", (int) ch) : ch);
				}
			}
			return expected.append('"').toString();
		}

		private byte[] encode(String content, Charset charset) {
			try {
				ByteBuffer buffer = charset.newEncoder()
					.onMalformedInput(CodingErrorAction.REPLACE)
					.onUnmappableCharacter(CodingErrorAction.REPLACE)
					.encode(CharBuffer.wrap(content));
				byte[] bytes = new byte[buffer.remaining()];
				buffer.get(bytes);
				return bytes;
			}
			catch (CharacterCodingException ex) {
				throw new IllegalStateException(ex);
			}
		}

		private byte[] bytes(String hex) {
			return HexFormat.ofDelimiter(" ").parseHex(hex);
		}

		private Appendable forwarding(StringBuilder target) {
			return new Appendable() {

				@Override
				public Appendable append(@Nullable CharSequence csq) {
					target.append(csq);
					return this;
				}

				@Override
				public Appendable append(@Nullable CharSequence csq, int start, int end) {
					target.append(csq, start, end);
					return this;
				}

				@Override
				public Appendable append(char c) {
					target.append(c);
					return this;
				}

			};
		}

		private Appendable throwingAppendable(boolean throwOnChar) {
			return new Appendable() {

				@Override
				public Appendable append(@Nullable CharSequence csq) throws IOException {
					throw new IOException("x");
				}

				@Override
				public Appendable append(@Nullable CharSequence csq, int start, int end) throws IOException {
					throw new IOException("x");
				}

				@Override
				public Appendable append(char c) throws IOException {
					if (throwOnChar) {
						throw new IOException("x");
					}
					return this;
				}

			};
		}

		static final class ToStringValue {

			private final @Nullable String toString;

			ToStringValue(@Nullable String toString) {
				this.toString = toString;
			}

			@Override
			@SuppressWarnings("NullAway") // Test null toString()
			public String toString() {
				return this.toString;
			}

		}

	}

	record Person(String firstName, @Nullable String lastName, int age) {

		@Override
		public String toString() {
			return "%s %s (%s)".formatted(this.firstName, this.lastName, this.age);
		}

	}

	record Couple(Person person1, Person person2) {

	}

}

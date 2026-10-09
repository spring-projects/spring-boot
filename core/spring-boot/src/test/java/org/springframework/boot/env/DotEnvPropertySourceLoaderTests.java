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

package org.springframework.boot.env;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.boot.origin.TextResourceOrigin;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link DotEnvPropertySourceLoader}.
 *
 * @author Sharang Gupta
 */
class DotEnvPropertySourceLoaderTests {

	private final DotEnvPropertySourceLoader loader = new DotEnvPropertySourceLoader();

	@Test
	void getFileExtensions() {
		assertThat(this.loader.getFileExtensions()).containsExactly("env");
	}

	@Test
	void loadReadsKeyValuePairs() throws Exception {
		PropertySource<?> source = load("""
				GREETING=hello
				TARGET=world
				""");
		assertThat(source.getProperty("GREETING")).isEqualTo("hello");
		assertThat(source.getProperty("TARGET")).isEqualTo("world");
	}

	@Test
	void loadIgnoresCommentsAndBlankLines() throws Exception {
		PropertySource<?> source = load("""
				# a comment

				   # an indented comment
				\t
				A=1
				""");
		assertThat(properties(source)).containsExactly(Map.entry("A", "1"));
	}

	@Test
	void loadIgnoresExportPrefix() throws Exception {
		PropertySource<?> source = load("""
				export A=1
				export   B=2
				EXPORTED=3
				""");
		assertThat(properties(source)).containsOnly(Map.entry("A", "1"), Map.entry("B", "2"),
				Map.entry("EXPORTED", "3"));
	}

	@Test
	void loadTrimsKeysAndUnquotedValues() throws Exception {
		PropertySource<?> source = load("  A  =  spaced value  \nB=\tvalue\t\n");
		assertThat(properties(source)).containsOnly(Map.entry("A", "spaced value"), Map.entry("B", "value"));
	}

	@Test
	void loadKeepsEqualsSignsInValue() throws Exception {
		PropertySource<?> source = load("URL=jdbc:postgresql://localhost/db?user=a&password=b==\n");
		assertThat(source.getProperty("URL")).isEqualTo("jdbc:postgresql://localhost/db?user=a&password=b==");
	}

	@ParameterizedTest
	@MethodSource("inlineCommentCases")
	void loadHandlesInlineCommentsOfUnquotedValue(String line, String expected) throws Exception {
		PropertySource<?> source = load(line + "\n");
		assertThat(source.getProperty("A")).isEqualTo(expected);
	}

	static Stream<Arguments> inlineCommentCases() {
		return Stream.of(Arguments.of("A=value # a comment", "value"), Arguments.of("A=value\t# a comment", "value"),
				Arguments.of("A=value#notacomment", "value#notacomment"),
				Arguments.of("A=#startswithhash", "#startswithhash"), Arguments.of("A=  # only a comment", ""),
				Arguments.of("A=a \"quoted\" b", "a \"quoted\" b"), Arguments.of("A=it's fine", "it's fine"));
	}

	@Test
	void loadReadsFollowingLinesAfterAnInlineComment() throws Exception {
		PropertySource<?> source = load("A=1 # first\nB=2 # second\nC=\"3\" # third\nD=4\nE=5 # last");
		assertThat(properties(source)).containsExactly(Map.entry("A", "1"), Map.entry("B", "2"), Map.entry("C", "3"),
				Map.entry("D", "4"), Map.entry("E", "5"));
		assertThat(origin(source, "D")).isEqualTo("4:3");
	}

	@Test
	void loadDoubleQuotedValuePreservesWhitespaceAndHash() throws Exception {
		PropertySource<?> source = load("A=\"  spaced # not a comment  \"\n");
		assertThat(source.getProperty("A")).isEqualTo("  spaced # not a comment  ");
	}

	@Test
	void loadDoubleQuotedValueProcessesEscapes() throws Exception {
		PropertySource<?> source = load("A=\"line1\\nline2\\tTabbed \\\"quoted\\\" back\\\\slash \\r\"\n");
		assertThat(source.getProperty("A")).isEqualTo("line1\nline2\tTabbed \"quoted\" back\\slash \r");
	}

	@Test
	void loadDoubleQuotedValueKeepsUnknownEscapes() throws Exception {
		PropertySource<?> source = load("A=\"C:\\Users\\\\x \\q\"\n");
		assertThat(source.getProperty("A")).isEqualTo("C:\\Users\\x \\q");
	}

	@Test
	void loadSingleQuotedValueIsLiteral() throws Exception {
		PropertySource<?> source = load("A='raw \\n \" # ${NOT_RESOLVED}'\n");
		assertThat(source.getProperty("A")).isEqualTo("raw \\n \" # ${NOT_RESOLVED}");
	}

	@Test
	void loadMultiLineDoubleQuotedValue() throws Exception {
		PropertySource<?> source = load("""
				KEY="-----BEGIN KEY-----
				abc
				-----END KEY-----"
				NEXT=after
				""");
		assertThat(source.getProperty("KEY")).isEqualTo("-----BEGIN KEY-----\nabc\n-----END KEY-----");
		assertThat(source.getProperty("NEXT")).isEqualTo("after");
	}

	@Test
	void loadMultiLineSingleQuotedValue() throws Exception {
		PropertySource<?> source = load("A='one\ntwo'\nB=3\n");
		assertThat(source.getProperty("A")).isEqualTo("one\ntwo");
		assertThat(source.getProperty("B")).isEqualTo("3");
	}

	@Test
	void loadAllowsCommentAfterClosingQuote() throws Exception {
		PropertySource<?> source = load("A=\"value\" # comment\nB='other'   \n");
		assertThat(source.getProperty("A")).isEqualTo("value");
		assertThat(source.getProperty("B")).isEqualTo("other");
	}

	@Test
	void loadEmptyValues() throws Exception {
		PropertySource<?> source = load("A=\nB=\"\"\nC=''\nD=   \n");
		assertThat(properties(source)).containsOnly(Map.entry("A", ""), Map.entry("B", ""), Map.entry("C", ""),
				Map.entry("D", ""));
	}

	@Test
	void loadLastDuplicateWins() throws Exception {
		PropertySource<?> source = load("A=first\nA=second\n");
		assertThat(source.getProperty("A")).isEqualTo("second");
	}

	@Test
	void loadHandlesAllLineSeparators() throws Exception {
		PropertySource<?> source = load("A=1\r\nB=2\rC=3\nD=\"x\r\ny\"\n");
		assertThat(properties(source)).containsOnly(Map.entry("A", "1"), Map.entry("B", "2"), Map.entry("C", "3"),
				Map.entry("D", "x\ny"));
	}

	@Test
	void loadSkipsByteOrderMark() throws Exception {
		PropertySource<?> source = load("\uFEFFA=1\n");
		assertThat(properties(source)).containsExactly(Map.entry("A", "1"));
	}

	@Test
	void loadReadsLastLineWithoutTrailingNewline() throws Exception {
		PropertySource<?> source = load("A=1\nB=2");
		assertThat(source.getProperty("B")).isEqualTo("2");
	}

	@Test
	void loadUsesUtf8ByDefault() throws Exception {
		PropertySource<?> source = load("A=caf\u00e9 \u20ac\n");
		assertThat(source.getProperty("A")).isEqualTo("caf\u00e9 \u20ac");
	}

	@Test
	void loadUsesSpecifiedEncoding() throws Exception {
		Resource resource = new ByteArrayResource("A=caf\u00e9\n".getBytes(StandardCharsets.ISO_8859_1));
		List<PropertySource<?>> loaded = this.loader.load("test.env", resource, StandardCharsets.ISO_8859_1);
		assertThat(loaded.get(0).getProperty("A")).isEqualTo("caf\u00e9");
	}

	@Test
	void loadReturnsEmptyListWhenThereAreNoProperties() throws Exception {
		assertThat(loadAll("")).isEmpty();
		assertThat(loadAll("# only a comment\n\n")).isEmpty();
	}

	@Test
	void loadReturnsSingleSystemEnvironmentPropertySource() throws Exception {
		List<PropertySource<?>> loaded = loadAll("A=1\n");
		assertThat(loaded).hasSize(1);
		assertThat(loaded.get(0)).isInstanceOf(SystemEnvironmentPropertySource.class);
		assertThat(loaded.get(0).getName()).isEqualTo("test.env-systemEnvironment");
	}

	@Test
	void loadedSourceResolvesPropertiesUsingEnvironmentVariableNaming() throws Exception {
		PropertySource<?> source = load("SPRING_DATASOURCE_URL=jdbc:h2:mem:test\n");
		assertThat(source.getProperty("SPRING_DATASOURCE_URL")).isEqualTo("jdbc:h2:mem:test");
		assertThat(source.getProperty("spring.datasource.url")).isEqualTo("jdbc:h2:mem:test");
		assertThat(source.containsProperty("spring.datasource.url")).isTrue();
		assertThat(source.getProperty("spring.datasource.username")).isNull();
	}

	@Test
	void loadedSourceCanBeBoundUsingRelaxedNames() throws Exception {
		PropertySource<?> source = load("""
				SPRING_DATASOURCE_URL=jdbc:h2:mem:test
				MY_SOME_KEY=bound
				my.dotted.key=dotted
				""");
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(source);
		Binder binder = Binder.get(environment);
		assertThat(binder.bind("spring.datasource.url", Bindable.of(String.class)).get()).isEqualTo("jdbc:h2:mem:test");
		assertThat(binder.bind("my.some-key", Bindable.of(String.class)).get()).isEqualTo("bound");
		assertThat(binder.bind("my.dotted.key", Bindable.of(String.class)).get()).isEqualTo("dotted");
	}

	@Test
	void loadedSourceIsImmutable() throws Exception {
		PropertySource<?> source = load("A=1\n");
		assertThat(source).isInstanceOfSatisfying(PropertySourceInfo.class,
				(info) -> assertThat(info.isImmutable()).isTrue());
	}

	@Test
	void loadTracksOriginOfEachValue() throws Exception {
		PropertySource<?> source = load("""
				# comment
				A=1
				export BB = "quoted"
				C=
				""");
		assertThat(origin(source, "A")).isEqualTo("2:3");
		assertThat(origin(source, "BB")).isEqualTo("3:13");
		assertThat(origin(source, "C")).isEqualTo("4:3");
		assertThat(origin(source, "MISSING")).isNull();
	}

	@Test
	void loadedOriginIsTheResource() throws Exception {
		Resource resource = new ByteArrayResource("A=1\n".getBytes(StandardCharsets.UTF_8));
		PropertySource<?> source = this.loader.load("test.env", resource).get(0);
		Origin origin = OriginLookup.getOrigin(source, "A");
		assertThat(origin).isInstanceOfSatisfying(TextResourceOrigin.class,
				(textOrigin) -> assertThat(textOrigin.getResource()).isSameAs(resource));
	}

	@ParameterizedTest
	@MethodSource("invalidContentCases")
	void loadWhenInvalidLineThrowsExceptionWithLineNumber(String content, int line) {
		assertThatIllegalStateException().isThrownBy(() -> load(content))
			.withMessageContaining("line " + line)
			.withMessageContaining("test.env");
	}

	static Stream<Arguments> invalidContentCases() {
		return Stream.of(Arguments.of("A=1\nJUSTAKEY\nB=2\n", 2), Arguments.of("A=1\n=value\n", 2),
				Arguments.of("A=1\nTWO WORDS=value\n", 2), Arguments.of("A=1\nexport\n", 2),
				Arguments.of("A=1\nexport \n", 2), Arguments.of("A=1\nB=\"value\" junk\n", 2),
				Arguments.of("A=1\nB='value' junk\n", 2), Arguments.of("A=1\n\nC=\"x\"y\n", 3));
	}

	@ParameterizedTest
	@MethodSource("secretContentCases")
	void loadWhenInvalidLineDoesNotIncludeFileContentInTheMessage(String content) {
		assertThatIllegalStateException().isThrownBy(() -> load(content))
			.satisfies((ex) -> assertThat(ex.getMessage()).doesNotContain("hunter2").doesNotContain("topsecret"));
	}

	static Stream<String> secretContentCases() {
		return Stream.of("hunter2\n", "my topsecret key=hunter2\n", "A=\"hunter2\" topsecret\n",
				"A='topsecret\nhunter2\n");
	}

	@Test
	void loadWhenUnterminatedMultiLineValueReportsLineWhereValueStarted() {
		assertThatIllegalStateException().isThrownBy(() -> load("A=1\nB=\"never\nclosed\nC=3\n"))
			.withMessageContaining("line 2")
			.withMessageContaining("unterminated");
	}

	private PropertySource<?> load(String content) throws IOException {
		List<PropertySource<?>> loaded = loadAll(content);
		assertThat(loaded).hasSize(1);
		return loaded.get(0);
	}

	private List<PropertySource<?>> loadAll(String content) throws IOException {
		return loadAll(content, StandardCharsets.UTF_8);
	}

	private List<PropertySource<?>> loadAll(String content, Charset charset) throws IOException {
		Resource resource = new ByteArrayResource(content.getBytes(charset)) {

			@Override
			public String getDescription() {
				return "test.env";
			}

		};
		return this.loader.load("test.env", resource);
	}

	private static Map<String, Object> properties(PropertySource<?> source) {
		Map<String, Object> properties = new java.util.LinkedHashMap<>();
		for (String name : ((org.springframework.core.env.EnumerablePropertySource<?>) source).getPropertyNames()) {
			properties.put(name, source.getProperty(name));
		}
		return properties;
	}

	private static @Nullable String origin(PropertySource<?> source, String name) {
		Origin origin = OriginLookup.getOrigin(source, name);
		if (origin instanceof TextResourceOrigin textOrigin) {
			return String.valueOf(textOrigin.getLocation());
		}
		return null;
	}

}

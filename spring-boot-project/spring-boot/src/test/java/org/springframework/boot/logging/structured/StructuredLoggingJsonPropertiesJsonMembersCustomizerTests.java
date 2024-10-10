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

package org.springframework.boot.logging.structured;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.json.JsonWriter.NameProcessor;
import org.springframework.boot.util.Instantiator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link StructuredLoggingJsonPropertiesJsonMembersCustomizer}.
 *
 * @author Phillip Webb
 */
@ExtendWith(MockitoExtension.class)
class StructuredLoggingJsonPropertiesJsonMembersCustomizerTests {

	@Mock
	private Instantiator<?> instantiator;

	@Test
	void customizeWhenHasExcludeFiltersMember() {
		StructuredLoggingJsonProperties properties = new StructuredLoggingJsonProperties(Collections.emptySet(),
				Set.of("a"), Collections.emptyMap(), Collections.emptyMap(), null);
		StructuredLoggingJsonPropertiesJsonMembersCustomizer customizer = new StructuredLoggingJsonPropertiesJsonMembersCustomizer(
				this.instantiator, properties);
		assertThat(writeSampleJson(customizer)).doesNotContain("a").contains("b");
	}

	@Test
	void customizeWhenHasIncludeFiltersOtherMembers() {
		StructuredLoggingJsonProperties properties = new StructuredLoggingJsonProperties(Set.of("a"),
				Collections.emptySet(), Collections.emptyMap(), Collections.emptyMap(), null);
		StructuredLoggingJsonPropertiesJsonMembersCustomizer customizer = new StructuredLoggingJsonPropertiesJsonMembersCustomizer(
				this.instantiator, properties);
		assertThat(writeSampleJson(customizer)).contains("a")
			.doesNotContain("b")
			.doesNotContain("c")
			.doesNotContain("d");
	}

	@Test
	void customizeWhenHasIncludeAndExcludeFiltersMembers() {
		StructuredLoggingJsonProperties properties = new StructuredLoggingJsonProperties(Set.of("a", "b"), Set.of("b"),
				Collections.emptyMap(), Collections.emptyMap(), null);
		StructuredLoggingJsonPropertiesJsonMembersCustomizer customizer = new StructuredLoggingJsonPropertiesJsonMembersCustomizer(
				this.instantiator, properties);
		assertThat(writeSampleJson(customizer)).contains("a")
			.doesNotContain("b")
			.doesNotContain("c")
			.doesNotContain("d");
	}

	@Test
	void customizeWhenHasRenameRenamesMember() {
		StructuredLoggingJsonProperties properties = new StructuredLoggingJsonProperties(Collections.emptySet(),
				Collections.emptySet(), Map.of("a", "z"), Collections.emptyMap(), null);
		StructuredLoggingJsonPropertiesJsonMembersCustomizer customizer = new StructuredLoggingJsonPropertiesJsonMembersCustomizer(
				this.instantiator, properties);
		assertThat(writeSampleJson(customizer)).contains("\"z\":\"a\"");
	}

	@Test
	void customizeWhenHasAddAddsMemeber() {
		StructuredLoggingJsonProperties properties = new StructuredLoggingJsonProperties(Collections.emptySet(),
				Collections.emptySet(), Collections.emptyMap(), Map.of("z", "z"), null);
		StructuredLoggingJsonPropertiesJsonMembersCustomizer customizer = new StructuredLoggingJsonPropertiesJsonMembersCustomizer(
				this.instantiator, properties);
		assertThat(writeSampleJson(customizer)).contains("\"z\":\"z\"");
	}

	@Test
	@SuppressWarnings("rawtypes")
	void customizeWhenHasCustomizerCustomizesMember() {
		StructureLoggingJsonMembersCustomizer<?> uppercaseCustomizer = (members) -> members
			.applyingNameProcessor(NameProcessor.of(String::toUpperCase));
		given(((Instantiator) this.instantiator).instantiate("test")).willReturn(uppercaseCustomizer);
		StructuredLoggingJsonProperties properties = new StructuredLoggingJsonProperties(Collections.emptySet(),
				Collections.emptySet(), Collections.emptyMap(), Collections.emptyMap(), "test");
		StructuredLoggingJsonPropertiesJsonMembersCustomizer customizer = new StructuredLoggingJsonPropertiesJsonMembersCustomizer(
				this.instantiator, properties);
		assertThat(writeSampleJson(customizer)).contains("\"A\":\"a\"");
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private String writeSampleJson(StructureLoggingJsonMembersCustomizer customizer) {
		return JsonWriter.of((members) -> {
			members.add("a", "a");
			members.add("b", "b");
			members.add("c", "c");
			customizer.customize(members);
		}).writeToString(new Object());
	}

}

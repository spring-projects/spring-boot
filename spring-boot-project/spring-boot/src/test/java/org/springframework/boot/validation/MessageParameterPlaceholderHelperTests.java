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

package org.springframework.boot.validation;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link MessageParameterPlaceholderHelper}.
 *
 * @author Dmytro Nosan
 */
class MessageParameterPlaceholderHelperTests {

	private final MessageParameterPlaceholderHelper resolver = new MessageParameterPlaceholderHelper();

	@Test
	void recursionInParameters() {
		Map<String, String> props = new LinkedHashMap<>();
		props.put("a", "{b}");
		props.put("b", "{c}");
		props.put("c", "{a}");
		assertThatIllegalArgumentException().isThrownBy(() -> this.resolver.replaceParameters("{a}", props::get))
				.withMessage("Circular reference '{a}'");
	}

	@Test
	void replaceParameters() {
		Map<String, String> props = new LinkedHashMap<>();
		props.put("foo", "fooValue");
		props.put("bar", "");
		assertThat(this.resolver.replaceParameters("{foo}{bar}", props::get)).isEqualTo("fooValue");
	}

	@Test
	void replaceNestedParameters() {
		Map<String, String> props = new LinkedHashMap<>();
		props.put("top", "{child}+{child}");
		props.put("child", "{{differentiator}.grandchild}");
		props.put("differentiator", "first");
		props.put("first.grandchild", "actualValue");
		assertThat(this.resolver.replaceParameters("{top}", props::get)).isEqualTo("actualValue+actualValue");
	}

	@Test
	void unresolvedParameters() {
		Map<String, String> props = new LinkedHashMap<>();
		props.put("top", "{child}+{child}");
		assertThat(this.resolver.replaceParameters("{foo}{top}{bar}", props::get))
				.isEqualTo("{foo}{child}+{child}{bar}");
	}

	@Test
	void unbalancedParentheses() {
		Map<String, String> props = new LinkedHashMap<>();
		props.put("top", "topValue");
		assertThat(this.resolver.replaceParameters("\\{top}", props::get)).isEqualTo("\\{top}");
		assertThat(this.resolver.replaceParameters("{top\\}", props::get)).isEqualTo("{top\\}");
		assertThat(this.resolver.replaceParameters("{{top}", props::get)).isEqualTo("{{top}");
		assertThat(this.resolver.replaceParameters("{top}}", props::get)).isEqualTo("topValue}");
	}

	@Test
	void resolveEscapeParameters() {
		Map<String, String> props = new LinkedHashMap<>();
		props.put("foo", "fooValue");
		props.put("bar", "\\{foo}");
		props.put("bazz\\}", "bazzValue");
		assertThat(this.resolver.replaceParameters("{foo}", props::get)).isEqualTo("fooValue");
		assertThat(this.resolver.replaceParameters("{foo}\\a", props::get)).isEqualTo("fooValue\\a");
		assertThat(this.resolver.replaceParameters("\\\\{foo}", props::get)).isEqualTo("\\\\fooValue");
		assertThat(this.resolver.replaceParameters("\\\\\\{foo}", props::get)).isEqualTo("\\\\\\{foo}");
		assertThat(this.resolver.replaceParameters("\\{foo}", props::get)).isEqualTo("\\{foo}");
		assertThat(this.resolver.replaceParameters("{foo\\}", props::get)).isEqualTo("{foo\\}");
		assertThat(this.resolver.replaceParameters("\\{foo\\}", props::get)).isEqualTo("\\{foo\\}");
		assertThat(this.resolver.replaceParameters("{foo}\\", props::get)).isEqualTo("fooValue\\");
		assertThat(this.resolver.replaceParameters("{bar}", props::get)).isEqualTo("\\{foo}");
		assertThat(this.resolver.replaceParameters("{bazz\\}}", props::get)).isEqualTo("bazzValue");
	}

}

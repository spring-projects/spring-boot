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

package org.springframework.boot.autoconfigure;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AutoConfigurationReplacements}.
 *
 * @author Phillip Webb
 */
class AutoConfigurationReplacementsTests {

	private final AutoConfigurationReplacements replacements = AutoConfigurationReplacements
		.load(TestAutoConfigurationReplacements.class, null);

	@Test
	void replaceWhenMatchReplacesClassName() {
		assertThat(this.replacements.replace("com.example.A1")).isEqualTo("com.example.A2");
	}

	@Test
	void replaceWhenNoMatchReturnsOriginalClassName() {
		assertThat(this.replacements.replace("com.example.Z1")).isEqualTo("com.example.Z1");
	}

	@Test
	void replaceAllReplacesAllMatching() {
		Set<String> classNames = new LinkedHashSet<>(
				List.of("com.example.A1", "com.example.B1", "com.example.Y1", "com.example.Z1"));
		assertThat(this.replacements.replaceAll(classNames)).containsExactly("com.example.A2", "com.example.B2",
				"com.example.Y1", "com.example.Z1");
	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@interface TestAutoConfigurationReplacements {

	}

}

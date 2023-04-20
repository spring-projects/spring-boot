/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.context.properties.bind

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.aot.hint.MemberCategory
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates


/**
 * Tests for {@link BindableRuntimeHintsRegistrar}.
 *
 * @author Andy Wilkinsin
 */
class KotlinBindableRuntimeHintsRegistrarTests {

	@Test
	fun `registerHints for data class with default value should allow declared constructors to be invoked`() {
		val runtimeHints = RuntimeHints()
		val register = BindableRuntimeHintsRegistrar.forTypes(PropertyWithDefaultValue::class.java)
		register.registerHints(runtimeHints)
		assertThat(runtimeHints.reflection().typeHints()).hasSize(1)
		assertThat(runtimeHints.reflection().typeHints()).allSatisfy { hint ->
			assertThat(hint.memberCategories).containsExactly(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)
		}
	}

	data class PropertyWithDefaultValue(var a: String = "alpha")
}
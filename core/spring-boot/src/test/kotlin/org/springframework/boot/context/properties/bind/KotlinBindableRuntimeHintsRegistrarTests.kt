/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.bind

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.aot.hint.MemberCategory
import org.springframework.aot.hint.RuntimeHints


/**
 * Tests for {@link BindableRuntimeHintsRegistrar}.
 *
 * @author Andy Wilkinson
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


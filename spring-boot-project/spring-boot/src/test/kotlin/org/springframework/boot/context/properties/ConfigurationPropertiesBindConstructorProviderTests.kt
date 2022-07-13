/*
 * Copyright 2012-2021 the original author or authors.
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
//
package org.springframework.boot.context.properties;

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * Tests for `ConfigurationPropertiesBindConstructorProvider`.
 *
 * @author Madhura Bhave
 */
@Suppress("unused")
class ConfigurationPropertiesBindConstructorProviderTests {

	private val constructorProvider = ConfigurationPropertiesBindConstructorProvider()

	@Test
	fun `type with default constructor should register java bean`() {
		val bindConstructor = this.constructorProvider.getBindConstructor(FooProperties::class.java, false)
		assertThat(bindConstructor).isNull()
	}

	@Test
	fun `type with no primary constructor should register java bean`() {
		val bindConstructor = this.constructorProvider.getBindConstructor(MultipleAmbiguousConstructors::class.java, false)
		assertThat(bindConstructor).isNull()
	}

	@Test
	fun `type with primary and secondary annotated constructor should use secondary constructor for binding`() {
		val bindConstructor = this.constructorProvider.getBindConstructor(ConstructorBindingOnSecondaryWithPrimaryConstructor::class.java, false)
		assertThat(bindConstructor).isNotNull();
	}

	@Test
	fun `type with primary constructor with autowired should not use constructor binding`() {
		val bindConstructor = this.constructorProvider.getBindConstructor(AutowiredPrimaryProperties::class.java, false)
		assertThat(bindConstructor).isNull()
	}

	@Test
	fun `type with primary and secondary constructor with autowired should not use constructor binding`() {
		val bindConstructor = this.constructorProvider.getBindConstructor(PrimaryWithAutowiredSecondaryProperties::class.java, false)
		assertThat(bindConstructor).isNull()
	}

	@Test
	fun `type with autowired secondary constructor should not use constructor binding`() {
		val bindConstructor = this.constructorProvider.getBindConstructor(AutowiredSecondaryProperties::class.java, false)
		assertThat(bindConstructor).isNull()
	}

	@Test
	fun `type with autowired primary and constructor binding on secondary constructor should throw exception`() {
		assertThatIllegalStateException().isThrownBy {
			this.constructorProvider.getBindConstructor(ConstructorBindingOnSecondaryAndAutowiredPrimaryProperties::class.java, false)
		}
	}

	@Test
	fun `type with autowired secondary and constructor binding on primary constructor should throw exception`() {
		assertThatIllegalStateException().isThrownBy {
			this.constructorProvider.getBindConstructor(ConstructorBindingOnPrimaryAndAutowiredSecondaryProperties::class.java, false)
		}
	}

	@Test
	fun `type with primary constructor and no annotation should use constructor binding`() {
		val bindConstructor = this.constructorProvider.getBindConstructor(ConstructorBindingPrimaryConstructorNoAnnotation::class.java, false)
		assertThat(bindConstructor).isNotNull()
	}

	@Test
	fun `type with secondary constructor and no annotation should use constructor binding`() {
		val bindConstructor = this.constructorProvider.getBindConstructor(ConstructorBindingSecondaryConstructorNoAnnotation::class.java, false)
		assertThat(bindConstructor).isNotNull()
	}

	@Test
	fun `type with multiple constructors`() {
		val bindConstructor = this.constructorProvider.getBindConstructor(ConstructorBindingMultipleConstructors::class.java, false)
		assertThat(bindConstructor).isNotNull()
	}

	@Test
	fun `type with multiple annotated constructors should throw exception`() {
		assertThatIllegalStateException().isThrownBy {
			this.constructorProvider.getBindConstructor(ConstructorBindingMultipleAnnotatedConstructors::class.java, false)
		}
	}

	@Test
	fun `type with secondary and primary annotated constructors should throw exception`() {
		assertThatIllegalStateException().isThrownBy {
			this.constructorProvider.getBindConstructor(ConstructorBindingSecondaryAndPrimaryAnnotatedConstructors::class.java, false)
		}
	}

	@ConfigurationProperties(prefix = "foo")
	class FooProperties

	@ConfigurationProperties(prefix = "bar")
	class PrimaryWithAutowiredSecondaryProperties constructor(val name: String?, val counter: Int = 42) {

		@Autowired
		constructor(@Suppress("UNUSED_PARAMETER") foo: String) : this(foo, 21)
	}

	@ConfigurationProperties(prefix = "bar")
	class AutowiredSecondaryProperties {

		@Autowired
		constructor(@Suppress("UNUSED_PARAMETER") foo: String)
	}

	@ConfigurationProperties(prefix = "bar")
	class AutowiredPrimaryProperties @Autowired constructor(val name: String?, val counter: Int = 42) {

	}

	@ConfigurationProperties(prefix = "bar")
	class ConstructorBindingOnSecondaryAndAutowiredPrimaryProperties @Autowired constructor(val name: String?, val counter: Int = 42) {

		@ConstructorBinding
		constructor(@Suppress("UNUSED_PARAMETER") foo: String) : this(foo, 21)
	}

	@ConfigurationProperties(prefix = "bar")
	class ConstructorBindingOnPrimaryAndAutowiredSecondaryProperties @ConstructorBinding constructor(val name: String?, val counter: Int = 42) {

		@Autowired
		constructor(@Suppress("UNUSED_PARAMETER") foo: String) : this(foo, 21)
	}

	@ConfigurationProperties(prefix = "bing")
	class ConstructorBindingOnSecondaryWithPrimaryConstructor constructor(val name: String?, val counter: Int = 42) {

		@ConstructorBinding
		constructor(@Suppress("UNUSED_PARAMETER") foo: String) : this(foo, 21)
	}

	@ConfigurationProperties(prefix = "bing")
	class ConstructorBindingOnPrimaryWithSecondaryConstructor @ConstructorBinding constructor(val name: String?, val counter: Int = 42) {

		constructor(@Suppress("UNUSED_PARAMETER") foo: String) : this(foo, 21)
	}

	@ConfigurationProperties(prefix = "bing")
	class ConstructorBindingPrimaryConstructorNoAnnotation(val name: String?, val counter: Int = 42)

	@ConfigurationProperties(prefix = "bing")
	class ConstructorBindingSecondaryConstructorNoAnnotation {

		constructor(@Suppress("UNUSED_PARAMETER") foo: String)

	}

	@ConfigurationProperties(prefix = "bing")
	class MultipleAmbiguousConstructors {

		constructor()

		constructor(@Suppress("UNUSED_PARAMETER") foo: String)

	}

	@ConfigurationProperties(prefix = "bing")
	class ConstructorBindingMultipleConstructors {

		constructor(@Suppress("UNUSED_PARAMETER") bar: Int)

		@ConstructorBinding
		constructor(@Suppress("UNUSED_PARAMETER") foo: String)

	}

	@ConfigurationProperties(prefix = "bing")
	class ConstructorBindingMultipleAnnotatedConstructors {

		@ConstructorBinding
		constructor(@Suppress("UNUSED_PARAMETER") bar: Int)

		@ConstructorBinding
		constructor(@Suppress("UNUSED_PARAMETER") foo: String)

	}

	@ConfigurationProperties(prefix = "bing")
	class ConstructorBindingSecondaryAndPrimaryAnnotatedConstructors @ConstructorBinding constructor(val name: String?, val counter: Int = 42) {

		@ConstructorBinding
		constructor(@Suppress("UNUSED_PARAMETER") foo: String) : this(foo, 21)

	}

}
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

package org.springframework.boot.context.properties

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.test.context.support.TestPropertySourceUtils

import org.assertj.core.api.Assertions.assertThat

/**
 * Tests for {@link ConfigurationProperties @ConfigurationProperties}-annotated beans.
 *
 * @author Madhura Bhave
 */
class KotlinConfigurationPropertiesTests {

	private var context = AnnotationConfigApplicationContext()

	@AfterEach
	fun cleanUp() {
		this.context.close();
	}

	@Test //gh-18652
	fun `type with constructor binding and existing singleton should not fail`() {
		val beanFactory = this.context.beanFactory
		(beanFactory as BeanDefinitionRegistry).registerBeanDefinition("foo",
				RootBeanDefinition(BingProperties::class.java))
		beanFactory.registerSingleton("foo", BingProperties(""))
		this.context.register(EnableConfigProperties::class.java)
		this.context.refresh();
	}

	@Test
	fun `type with constructor bound lateinit property can be bound`() {
		this.context.register(EnableLateInitProperties::class.java)
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context, "lateinit.inner.value=alpha");
		this.context.refresh();
		assertThat(this.context.getBean(LateInitProperties::class.java).inner.value).isEqualTo("alpha")
	}

	@Test
	fun `type with constructor bound lateinit property with default can be bound`() {
		this.context.register(EnableLateInitPropertiesWithDefault::class.java)
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context, "lateinit-with-default.inner.bravo=two");
		this.context.refresh();
		val properties = this.context.getBean(LateInitPropertiesWithDefault::class.java)
		assertThat(properties.inner.alpha).isEqualTo("apple")
		assertThat(properties.inner.bravo).isEqualTo("two")
	}

	@Test
	fun `mutable data class properties can be imported`() {
		this.context.register(MutableDataClassPropertiesImporter::class.java)
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context, "mutable.prop=alpha");
		this.context.refresh();
		assertThat(this.context.getBean(MutableDataClassProperties::class.java).prop).isEqualTo("alpha")
	}

	@ConfigurationProperties(prefix = "foo")
	class BingProperties(@Suppress("UNUSED_PARAMETER") bar: String) {

	}

	@EnableConfigurationProperties
	class EnableConfigProperties {

	}

	@ConfigurationProperties("lateinit")
	class LateInitProperties {

		lateinit var inner: Inner

	}

	data class Inner(val value: String)

	@EnableConfigurationProperties(LateInitPropertiesWithDefault::class)
	class EnableLateInitPropertiesWithDefault {

	}

	@ConfigurationProperties("lateinit-with-default")
	class LateInitPropertiesWithDefault {

		lateinit var inner: InnerWithDefault

	}

	data class InnerWithDefault(val alpha: String = "apple", val bravo: String = "banana")

	@EnableConfigurationProperties(LateInitProperties::class)
	class EnableLateInitProperties {

	}

	@EnableConfigurationProperties
	@Configuration(proxyBeanMethods = false)
	@Import(MutableDataClassProperties::class)
	class MutableDataClassPropertiesImporter {
	}

	@ConfigurationProperties(prefix = "mutable")
	data class MutableDataClassProperties(
		var prop: String = ""
	)

}
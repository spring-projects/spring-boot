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

package org.springframework.boot.context.properties

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.boot.context.properties.bind.BindMethod

/**
 * Tests for `ConfigurationPropertiesBeanRegistrar`.
 *
 * @author Stephane Nicoll
 */
@Suppress("unused")
class KotlinConfigurationPropertiesBeanRegistrarTests {

	private val beanFactory = DefaultListableBeanFactory()

	private val registrar = ConfigurationPropertiesBeanRegistrar(beanFactory)

	@Test
	fun `type with primary constructor and no autowired should register value object configuration properties`() {
		this.registrar.register(BarProperties::class.java)
		val beanDefinition = this.beanFactory.getBeanDefinition(
				"bar-org.springframework.boot.context.properties.KotlinConfigurationPropertiesBeanRegistrarTests\$BarProperties")
		assertThat(beanDefinition.hasAttribute(BindMethod::class.java.name)).isTrue()
		assertThat(beanDefinition.getAttribute(BindMethod::class.java.name)).isEqualTo(BindMethod.VALUE_OBJECT)
	}

	@Test
	fun `type with no primary constructor should register java bean configuration properties`() {
		this.registrar.register(BingProperties::class.java)
		val beanDefinition = this.beanFactory.getBeanDefinition(
				"bing-org.springframework.boot.context.properties.KotlinConfigurationPropertiesBeanRegistrarTests\$BingProperties")
		assertThat(beanDefinition.hasAttribute(BindMethod::class.java.name)).isTrue()
		assertThat(beanDefinition.getAttribute(BindMethod::class.java.name)).isEqualTo(BindMethod.JAVA_BEAN)
	}

	@ConfigurationProperties("foo")
	class FooProperties

	@ConfigurationProperties("bar")
	class BarProperties(val name: String?, val counter: Int = 42)

	@ConfigurationProperties("bing")
	class BingProperties {

		constructor()

		constructor(@Suppress("UNUSED_PARAMETER") foo: String)

	}

}

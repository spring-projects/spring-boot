package org.springframework.boot.context.properties

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.beans.factory.support.RootBeanDefinition

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
	fun `type with default constructor should register root bean definition`() {
		this.registrar.register(FooProperties::class.java)
		val beanDefinition = this.beanFactory.getBeanDefinition(
				"foo-org.springframework.boot.context.properties.KotlinConfigurationPropertiesBeanRegistrarTests\$FooProperties")
		assertThat(beanDefinition).isExactlyInstanceOf(RootBeanDefinition::class.java)
	}

	@Test
	fun `type with primary constructor and no autowired should register configuration properties bean definition`() {
		this.registrar.register(BarProperties::class.java)
		val beanDefinition = this.beanFactory.getBeanDefinition(
				"bar-org.springframework.boot.context.properties.KotlinConfigurationPropertiesBeanRegistrarTests\$BarProperties")
		assertThat(beanDefinition.hasAttribute(ConfigurationPropertiesBean.BindMethod::class.java.name)).isTrue()
		assertThat(beanDefinition.getAttribute(ConfigurationPropertiesBean.BindMethod::class.java.name))
				.isEqualTo(ConfigurationPropertiesBean.BindMethod.VALUE_OBJECT)
	}

	@Test
	fun `type with no primary constructor should register root bean definition`() {
		this.registrar.register(BingProperties::class.java)
		val beanDefinition = this.beanFactory.getBeanDefinition(
				"bing-org.springframework.boot.context.properties.KotlinConfigurationPropertiesBeanRegistrarTests\$BingProperties")
		assertThat(beanDefinition).isExactlyInstanceOf(RootBeanDefinition::class.java)
	}

	@ConfigurationProperties(prefix = "foo")
	class FooProperties

	@ConfigurationProperties(prefix = "bar")
	class BarProperties(val name: String?, val counter: Int = 42)

	@ConfigurationProperties(prefix = "bing")
	class BingProperties {

		constructor()

		constructor(@Suppress("UNUSED_PARAMETER") foo: String)

	}

}

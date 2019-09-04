package org.springframework.boot.context.properties

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.beans.factory.support.GenericBeanDefinition
import org.springframework.core.type.AnnotationMetadata
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory

/**
 * Tests for `ConfigurationPropertiesBeanRegistrar`.
 *
 * @author Stephane Nicoll
 */
@Suppress("unused")
class KotlinConfigurationPropertiesBeanRegistrarTests {

	private val registrar = ConfigurationPropertiesBeanRegistrar()

	private val beanFactory = DefaultListableBeanFactory()

	@Test
	fun `type with default constructor should register generic bean definition`() {
		this.registrar.registerBeanDefinitions(
				getAnnotationMetadata(TestConfiguration::class.java), this.beanFactory)
		val beanDefinition = this.beanFactory.getBeanDefinition(
				"foo-org.springframework.boot.context.properties.KotlinConfigurationPropertiesBeanRegistrarTests\$FooProperties")
		assertThat(beanDefinition).isExactlyInstanceOf(GenericBeanDefinition::class.java)
	}

	@Test
	fun `type with primary constructor and no autowired should register configuration properties bean definition`() {
		this.registrar.registerBeanDefinitions(
				getAnnotationMetadata(TestConfiguration::class.java), this.beanFactory)
		val beanDefinition = this.beanFactory.getBeanDefinition(
				"bar-org.springframework.boot.context.properties.KotlinConfigurationPropertiesBeanRegistrarTests\$BarProperties")
		assertThat(beanDefinition).isExactlyInstanceOf(
				ConfigurationPropertiesBeanDefinition::class.java)
	}

	@Test
	fun `type with no primary constructor should register generic bean definition`() {
		this.registrar.registerBeanDefinitions(
				getAnnotationMetadata(TestConfiguration::class.java), this.beanFactory)
		val beanDefinition = this.beanFactory.getBeanDefinition(
				"bing-org.springframework.boot.context.properties.KotlinConfigurationPropertiesBeanRegistrarTests\$BingProperties")
		assertThat(beanDefinition).isExactlyInstanceOf(GenericBeanDefinition::class.java)
	}

	private fun getAnnotationMetadata(source: Class<*>): AnnotationMetadata {
		return SimpleMetadataReaderFactory().getMetadataReader(source.name)
				.annotationMetadata
	}


	@EnableConfigurationProperties(FooProperties::class, BarProperties::class,
			BingProperties::class)
	class TestConfiguration

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

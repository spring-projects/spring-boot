package org.springframework.boot.context.properties

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Configuration

/**
 * Tests for {@link ConfigurationProperties @ConfigurationProperties}-annotated beans.
 *
 * @author Madhura Bhave
 */
class KotlinConfigurationPropertiesTests {

	private var context = AnnotationConfigApplicationContext()

	@Test //gh-18652
	fun `type with constructor binding and existing singleton should not fail`() {
		val beanFactory = this.context.beanFactory
		(beanFactory as BeanDefinitionRegistry).registerBeanDefinition("foo",
				RootBeanDefinition(BingProperties::class.java))
		beanFactory.registerSingleton("foo", BingProperties(""))
		this.context.register(TestConfig::class.java)
		this.context.refresh();
	}

	@ConfigurationProperties(prefix = "foo")
	@ConstructorBinding
	class BingProperties(@Suppress("UNUSED_PARAMETER") bar: String) {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	internal open class TestConfig {

	}

}
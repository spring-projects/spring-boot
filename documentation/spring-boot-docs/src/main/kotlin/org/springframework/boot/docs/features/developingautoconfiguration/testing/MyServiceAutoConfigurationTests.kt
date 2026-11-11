/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.developingautoconfiguration.testing

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.FilteredClassLoader
import org.springframework.boot.test.context.assertj.AssertableApplicationContext
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

internal open class MyServiceAutoConfigurationTests {

	// tag::runner[]
	val contextRunner = ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(MyServiceAutoConfiguration::class.java))

	// end::runner[]

	// tag::test-env[]
	@Test
	fun serviceNameCanBeConfigured() {
		contextRunner.withPropertyValues("user.name=test123").run { context: AssertableApplicationContext ->
			assertThat(context).hasSingleBean(MyService::class.java)
			assertThat(context.getBean(MyService::class.java).name).isEqualTo("test123")
		}
	}
	// end::test-env[]

	// tag::test-classloader[]
	@Test
	fun serviceIsIgnoredIfLibraryIsNotPresent() {
		contextRunner.withClassLoader(FilteredClassLoader(MyService::class.java))
			.run { context: AssertableApplicationContext? ->
				assertThat(context).doesNotHaveBean("myService")
			}
	}
	// end::test-classloader[]

	// tag::test-user-config[]
	@Test
	fun defaultServiceBacksOff() {
		contextRunner.withUserConfiguration(UserConfiguration::class.java)
			.run { context: AssertableApplicationContext ->
				assertThat(context).hasSingleBean(MyService::class.java)
				assertThat(context).getBean("myCustomService")
					.isSameAs(context.getBean(MyService::class.java))
			}
	}

	@Configuration(proxyBeanMethods = false)
	internal class UserConfiguration {

		@Bean
		fun myCustomService(): MyService {
			return MyService("mine")
		}

	}
	// end::test-user-config[]
}


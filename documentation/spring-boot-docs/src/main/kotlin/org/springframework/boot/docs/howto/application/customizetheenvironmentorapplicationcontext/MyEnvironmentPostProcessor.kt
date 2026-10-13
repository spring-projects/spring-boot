/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.application.customizetheenvironmentorapplicationcontext

import org.springframework.boot.EnvironmentPostProcessor
import org.springframework.boot.SpringApplication
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.PropertySource
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.util.Assert
import java.io.IOException

class MyEnvironmentPostProcessor : EnvironmentPostProcessor {

	private val loader = YamlPropertySourceLoader()

	override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
		val path: Resource = ClassPathResource("com/example/myapp/config.yml")
		val propertySource = loadYaml(path)
		environment.propertySources.addLast(propertySource)
	}

	private fun loadYaml(path: Resource): PropertySource<*> {
		Assert.isTrue(path.exists()) { "'path' [$path] must exist" }
		return try {
			loader.load("custom-resource", path)[0]
		} catch (ex: IOException) {
			throw IllegalStateException("Failed to load yaml configuration from $path", ex)
		}
	}

}


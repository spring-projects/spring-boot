/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.docs.howto.application.customizetheenvironmentorapplicationcontext

import org.springframework.boot.SpringApplication
import org.springframework.boot.env.EnvironmentPostProcessor
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
		Assert.isTrue(path.exists()) { "Resource $path does not exist" }
		return try {
			loader.load("custom-resource", path)[0]
		} catch (ex: IOException) {
			throw IllegalStateException("Failed to load yaml configuration from $path", ex)
		}
	}

}

/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.docs.features.devservices.testcontainers.atdevelopmenttime.dynamicproperties

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.mongodb.MongoDBContainer

@TestConfiguration(proxyBeanMethods = false)
class MyContainersConfiguration {

	@Bean
	fun mongoDbContainer(): MongoDBContainer {
		return MongoDBContainer("mongo:5.0")
	}
	
	@Bean
	fun mongoDbProperties(container: MongoDBContainer): DynamicPropertyRegistrar {
		return DynamicPropertyRegistrar { properties ->
			properties.add("spring.mongodb.host") { container.host }
			properties.add("spring.mongodb.port") { container.firstMappedPort }
		}
	}

}


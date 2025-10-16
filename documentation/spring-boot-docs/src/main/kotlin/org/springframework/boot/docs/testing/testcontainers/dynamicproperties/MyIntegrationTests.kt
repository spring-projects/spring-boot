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
package org.springframework.boot.docs.testing.testcontainers.dynamicproperties

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.neo4j.Neo4jContainer

@Testcontainers
@SpringBootTest
class MyIntegrationTests {

	@Test
	fun myTest() {
		/**/ println()
	}

	companion object {
		@Container
		@JvmStatic
		val neo4j = Neo4jContainer("neo4j:5");

		@DynamicPropertySource
		@JvmStatic
		fun neo4jProperties(registry: DynamicPropertyRegistry) {
			registry.add("spring.neo4j.uri") { neo4j.boltUrl }
		}
	}
}


/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.docs.features.testing.springbootapplications.jsontests

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest
import org.springframework.boot.test.json.JacksonTester

@JsonTest
class MyJsonTests(@Autowired val json: JacksonTester<VehicleDetails>) {
	@Test
	fun serialize() {
		val details = VehicleDetails("Honda", "Civic")
		// Assert against a `.json` file in the same package as the test
		Assertions.assertThat(json.write(details)).isEqualToJson("expected.json")
		// Or use JSON path based assertions
		Assertions.assertThat(json.write(details)).hasJsonPathStringValue("@.make")
		Assertions.assertThat(json.write(details)).extractingJsonPathStringValue("@.make").isEqualTo("Honda")
	}

	@Test
	fun deserialize() {
		val content = "{\"make\":\"Ford\",\"model\":\"Focus\"}"
		Assertions.assertThat(
			json.parse(content)
		).isEqualTo(VehicleDetails("Ford", "Focus"))
		Assertions.assertThat(json.parseObject(content).make).isEqualTo("Ford")
	}
}
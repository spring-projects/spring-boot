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

package org.springframework.boot.docs.testing.springbootapplications.xmltests

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.xml.XmlTest
import org.springframework.boot.test.xml.JacksonXmlTester

@XmlTest
class MyXmlTests(@Autowired val xml: JacksonXmlTester<VehicleDetails>) {

	@Test
	fun serialize() {
		val details = VehicleDetails("Honda", "Civic")
		// Assert against a `.xml` file in the same package as the test
		assertThat(xml.write(details)).isSimilarToXml("expected.xml")
		// Or use XPath based assertions
		assertThat(xml.write(details)).hasXPathValue("/VehicleDetails/make")
		assertThat(xml.write(details)).extractingXPathStringValue("/VehicleDetails/make").isEqualTo("Honda")
	}

	@Test
	fun deserialize() {
		val content = "<VehicleDetails><make>Ford</make><model>Focus</model></VehicleDetails>"
		assertThat(xml.parse(content)).isEqualTo(VehicleDetails("Ford", "Focus"))
		assertThat(xml.parseObject(content).make).isEqualTo("Ford")
	}

}

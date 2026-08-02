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

package org.springframework.boot.docs.testing.springbootapplications.xmltests;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.xml.XmlTest;
import org.springframework.boot.test.xml.JacksonXmlTester;

import static org.assertj.core.api.Assertions.assertThat;

@XmlTest
class MyXmlTests {

	@Autowired
	private JacksonXmlTester<VehicleDetails> xml;

	@Test
	void serialize() throws Exception {
		VehicleDetails details = new VehicleDetails("Honda", "Civic");
		// Assert against a `.xml` file in the same package as the test
		assertThat(this.xml.write(details)).isSimilarToXml("expected.xml");
		// Or use XPath based assertions
		assertThat(this.xml.write(details)).hasXPathValue("/VehicleDetails/make");
		assertThat(this.xml.write(details)).extractingXPathStringValue("/VehicleDetails/make").isEqualTo("Honda");
	}

	@Test
	void deserialize() throws Exception {
		String content = "<VehicleDetails><make>Ford</make><model>Focus</model></VehicleDetails>";
		assertThat(this.xml.parse(content)).isEqualTo(new VehicleDetails("Ford", "Focus"));
		assertThat(this.xml.parseObject(content).getMake()).isEqualTo("Ford");
	}

}

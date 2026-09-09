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

package org.springframework.boot.maven;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.w3c.dom.Document;

import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the Eclipse m2e lifecycle mapping metadata.
 *
 * @author Wan bin yu
 */
class LifecycleMappingMetadataTests {

	@ParameterizedTest
	@CsvSource({ "build-info,true,false", "process-aot,false,true" })
	void goalsHaveExpectedExecutionSettings(String goal, String runOnIncremental, String runOnConfiguration)
			throws Exception {
		try (InputStream input = new ClassPathResource("META-INF/m2e/lifecycle-mapping-metadata.xml")
			.getInputStream()) {
			Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input);
			XPath xpath = XPathFactory.newInstance().newXPath();
			String execution = "/lifecycleMappingMetadata/pluginExecutions/pluginExecution[pluginExecutionFilter/goals/goal='"
					+ goal + "']";
			assertThat(xpath.evaluate("count(" + execution + ")", document)).isEqualTo("1");
			assertThat(xpath.evaluate(execution + "/action/execute/runOnIncremental", document))
				.isEqualTo(runOnIncremental);
			assertThat(xpath.evaluate(execution + "/action/execute/runOnConfiguration", document))
				.isEqualTo(runOnConfiguration);
		}
	}

}

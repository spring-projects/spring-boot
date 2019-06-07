/*
 * Copyright 2012-2019 the original author or authors.
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
package org.springframework.boot.actuate.autoconfigure.web.server;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.servlet.ServletManagementContextAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ManagementContextAutoConfiguration}.
 *
 * @author Madhura Bhave
 * @author Andy Wilkinson
 */
public class ManagementContextAutoConfigurationTests {

	@Rule
	public OutputCapture output = new OutputCapture();

	@Test
	public void childManagementContextShouldStartForEmbeddedServer() {
		WebApplicationContextRunner contextRunner = new WebApplicationContextRunner(
				AnnotationConfigServletWebServerApplicationContext::new)
						.withConfiguration(AutoConfigurations.of(ManagementContextAutoConfiguration.class,
								ServletWebServerFactoryAutoConfiguration.class,
								ServletManagementContextAutoConfiguration.class, WebEndpointAutoConfiguration.class,
								EndpointAutoConfiguration.class));
		contextRunner.withPropertyValues("server.port=0", "management.server.port=0")
				.run((context) -> assertThat(tomcatStartedOccurencesIn(this.output.toString())).isEqualTo(2));

	}

	private int tomcatStartedOccurencesIn(String output) {
		int matches = 0;
		Matcher matcher = Pattern.compile("Tomcat started on port").matcher(output);
		while (matcher.find()) {
			matches++;
		}
		return matches;
	}

}

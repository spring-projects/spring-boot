/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.freemarker;

import java.io.File;
import java.io.StringWriter;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.rule.OutputCapture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 * Tests for {@link FreeMarkerAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Kazuki Shimizu
 */
public class FreeMarkerAutoConfigurationTests {

	@Rule
	public OutputCapture output = new OutputCapture();

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(FreeMarkerAutoConfiguration.class));

	@Test
	public void renderNonWebAppTemplate() {
		this.contextRunner.run((context) -> {
			freemarker.template.Configuration freemarker = context
					.getBean(freemarker.template.Configuration.class);
			StringWriter writer = new StringWriter();
			freemarker.getTemplate("message.ftl").process(this, writer);
			assertThat(writer.toString()).contains("Hello World");
		});
	}

	public String getGreeting() {
		return "Hello World";
	}

	@Test
	public void nonExistentTemplateLocation() {
		this.contextRunner
				.withPropertyValues("spring.freemarker.templateLoaderPath:"
						+ "classpath:/does-not-exist/,classpath:/also-does-not-exist")
				.run((context) -> this.output
						.expect(containsString("Cannot find template location")));
	}

	@Test
	public void emptyTemplateLocation() {
		new File("target/test-classes/templates/empty-directory").mkdir();
		this.contextRunner.withPropertyValues("spring.freemarker.templateLoaderPath:"
				+ "classpath:/templates/empty-directory/").run((context) -> {
				});
	}

	@Test
	public void nonExistentLocationAndEmptyLocation() {
		new File("target/test-classes/templates/empty-directory").mkdir();
		this.contextRunner.withPropertyValues("spring.freemarker.templateLoaderPath:"
				+ "classpath:/does-not-exist/,classpath:/templates/empty-directory/")
				.run((context) -> {
				});
	}

}

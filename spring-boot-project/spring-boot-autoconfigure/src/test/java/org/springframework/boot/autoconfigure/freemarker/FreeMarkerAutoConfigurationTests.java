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

package org.springframework.boot.autoconfigure.freemarker;

import java.io.File;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testsupport.BuildOutput;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FreeMarkerAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Kazuki Shimizu
 */
@ExtendWith(OutputCaptureExtension.class)
public class FreeMarkerAutoConfigurationTests {

	private final BuildOutput buildOutput = new BuildOutput(getClass());

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(FreeMarkerAutoConfiguration.class));

	@Test
	void renderNonWebAppTemplate() {
		this.contextRunner.run((context) -> {
			freemarker.template.Configuration freemarker = context.getBean(freemarker.template.Configuration.class);
			StringWriter writer = new StringWriter();
			freemarker.getTemplate("message.ftlh").process(new DataModel(), writer);
			assertThat(writer.toString()).contains("Hello World");
		});
	}

	@Test
	void nonExistentTemplateLocation(CapturedOutput output) {
		this.contextRunner
				.withPropertyValues("spring.freemarker.templateLoaderPath:"
						+ "classpath:/does-not-exist/,classpath:/also-does-not-exist")
				.run((context) -> assertThat(output).contains("Cannot find template location"));
	}

	@Test
	void emptyTemplateLocation(CapturedOutput output) {
		File emptyDirectory = new File(this.buildOutput.getTestResourcesLocation(), "empty-templates/empty-directory");
		emptyDirectory.mkdirs();
		this.contextRunner
				.withPropertyValues("spring.freemarker.templateLoaderPath:classpath:/empty-templates/empty-directory/")
				.run((context) -> assertThat(output).doesNotContain("Cannot find template location"));
	}

	@Test
	void nonExistentLocationAndEmptyLocation(CapturedOutput output) {
		new File(this.buildOutput.getTestResourcesLocation(), "empty-templates/empty-directory").mkdirs();
		this.contextRunner
				.withPropertyValues("spring.freemarker.templateLoaderPath:"
						+ "classpath:/does-not-exist/,classpath:/empty-templates/empty-directory/")
				.run((context) -> assertThat(output).doesNotContain("Cannot find template location"));
	}

	public static class DataModel {

		public String getGreeting() {
			return "Hello World";
		}

	}

}

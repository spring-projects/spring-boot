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

package org.springframework.boot.actuate.autoconfigure.logging;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.boot.actuate.logging.LogFileWebEndpoint;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LogFileWebEndpointAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Christian Carriere-Tisseur
 */
public class LogFileWebEndpointAutoConfigurationTests {

	private WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withUserConfiguration(LogFileWebEndpointAutoConfiguration.class);

	@Rule
	public final TemporaryFolder temp = new TemporaryFolder();

	@Test
	public void runWithOnlyExposedShouldNotHaveEndpointBean() {
		this.contextRunner
				.withPropertyValues("management.endpoints.web.exposure.include=logfile")
				.run((context) -> assertThat(context)
						.doesNotHaveBean(LogFileWebEndpoint.class));
	}

	@Test
	public void runWhenLoggingFileIsSetAndNotExposedShouldNotHaveEndpointBean() {
		this.contextRunner.withPropertyValues("logging.file.name:test.log")
				.run((context) -> assertThat(context)
						.doesNotHaveBean(LogFileWebEndpoint.class));
	}

	@Test
	public void runWhenLoggingFileIsSetAndExposedShouldHaveEndpointBean() {
		this.contextRunner
				.withPropertyValues("logging.file.name:test.log",
						"management.endpoints.web.exposure.include=logfile")
				.run((context) -> assertThat(context)
						.hasSingleBean(LogFileWebEndpoint.class));
	}

	@Test
	@Deprecated
	public void runWhenLoggingFileIsSetWithDeprecatedPropertyAndExposedShouldHaveEndpointBean() {
		this.contextRunner
				.withPropertyValues("logging.file:test.log",
						"management.endpoints.web.exposure.include=logfile")
				.run((context) -> assertThat(context)
						.hasSingleBean(LogFileWebEndpoint.class));
	}

	@Test
	public void runWhenLoggingPathIsSetAndNotExposedShouldNotHaveEndpointBean() {
		this.contextRunner.withPropertyValues("logging.file.path:test/logs")
				.run((context) -> assertThat(context)
						.doesNotHaveBean(LogFileWebEndpoint.class));
	}

	@Test
	public void runWhenLoggingPathIsSetAndExposedShouldHaveEndpointBean() {
		this.contextRunner
				.withPropertyValues("logging.file.path:test/logs",
						"management.endpoints.web.exposure.include=logfile")
				.run((context) -> assertThat(context)
						.hasSingleBean(LogFileWebEndpoint.class));
	}

	@Test
	@Deprecated
	public void runWhenLoggingPathIsSetWithDeprecatedPropertyAndExposedShouldHaveEndpointBean() {
		this.contextRunner
				.withPropertyValues("logging.path:test/logs",
						"management.endpoints.web.exposure.include=logfile")
				.run((context) -> assertThat(context)
						.hasSingleBean(LogFileWebEndpoint.class));
	}

	@Test
	public void logFileWebEndpointIsAutoConfiguredWhenExternalFileIsSet() {
		this.contextRunner
				.withPropertyValues(
						"management.endpoint.logfile.external-file:external.log",
						"management.endpoints.web.exposure.include=logfile")
				.run((context) -> assertThat(context)
						.hasSingleBean(LogFileWebEndpoint.class));
	}

	@Test
	public void logFileWebEndpointCanBeDisabled() {
		this.contextRunner
				.withPropertyValues("logging.file.name:test.log",
						"management.endpoint.logfile.enabled:false")
				.run((context) -> assertThat(context)
						.doesNotHaveBean(LogFileWebEndpoint.class));
	}

	@Test
	public void logFileWebEndpointUsesConfiguredExternalFile() throws IOException {
		File file = this.temp.newFile();
		FileCopyUtils.copy("--TEST--".getBytes(), file);
		this.contextRunner.withPropertyValues(
				"management.endpoints.web.exposure.include=logfile",
				"management.endpoint.logfile.external-file:" + file.getAbsolutePath())
				.run((context) -> {
					assertThat(context).hasSingleBean(LogFileWebEndpoint.class);
					LogFileWebEndpoint endpoint = context
							.getBean(LogFileWebEndpoint.class);
					Resource resource = endpoint.logFile();
					assertThat(resource).isNotNull();
					assertThat(StreamUtils.copyToString(resource.getInputStream(),
							StandardCharsets.UTF_8)).isEqualTo("--TEST--");
				});
	}

}

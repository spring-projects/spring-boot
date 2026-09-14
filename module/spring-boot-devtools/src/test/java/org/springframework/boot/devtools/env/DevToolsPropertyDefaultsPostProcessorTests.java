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

package org.springframework.boot.devtools.env;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.logging.DeferredLog;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.context.reactive.StandardReactiveWebEnvironment;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.support.StandardServletEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DevToolsPropertyDefaultsPostProcessor}.
 *
 * @author ohchanKyu
 */
@ExtendWith(OutputCaptureExtension.class)
class DevToolsPropertyDefaultsPostProcessorTests {

	private static final String WEB_LOGGING_HINT = "For additional web related logging consider setting the "
			+ "'logging.level.web' property to 'DEBUG'";

	@Test
	void postProcessWhenServletWebEnvironmentLogsWebLoggingHint(CapturedOutput output) throws Exception {
		postProcess(new StandardServletEnvironment());
		assertThat(output).contains(WEB_LOGGING_HINT);
	}

	@Test
	void postProcessWhenReactiveWebEnvironmentLogsWebLoggingHint(CapturedOutput output) throws Exception {
		postProcess(new StandardReactiveWebEnvironment());
		assertThat(output).contains(WEB_LOGGING_HINT);
	}

	@Test
	void postProcessWhenNonWebEnvironmentDoesNotLogWebLoggingHint(CapturedOutput output) throws Exception {
		postProcess(new StandardEnvironment());
		assertThat(output).doesNotContain(WEB_LOGGING_HINT);
	}

	@Test
	void postProcessWhenWebLoggingIsConfiguredDoesNotLogWebLoggingHint(CapturedOutput output) throws Exception {
		StandardReactiveWebEnvironment environment = new StandardReactiveWebEnvironment();
		TestPropertyValues.of("logging.level.web=DEBUG").applyTo(environment);
		postProcess(environment);
		assertThat(output).doesNotContain(WEB_LOGGING_HINT);
	}

	private void postProcess(ConfigurableEnvironment environment) throws Exception {
		DevToolsPropertyDefaultsPostProcessor postProcessor = new DevToolsPropertyDefaultsPostProcessor();
		// Run in a new thread so that DevTools is not disabled by the test runner
		Thread thread = new Thread(() -> postProcessor.postProcessEnvironment(environment, new SpringApplication()));
		thread.start();
		thread.join();
		replayDeferredLog();
	}

	private void replayDeferredLog() {
		Object logger = ReflectionTestUtils.getField(DevToolsPropertyDefaultsPostProcessor.class, "logger");
		assertThat(logger).isInstanceOf(DeferredLog.class);
		((DeferredLog) logger).switchTo(DevToolsPropertyDefaultsPostProcessor.class);
	}

}

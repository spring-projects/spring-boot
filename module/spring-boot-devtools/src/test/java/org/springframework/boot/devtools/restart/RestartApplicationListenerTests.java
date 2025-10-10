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

package org.springframework.boot.devtools.restart;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.bootstrap.DefaultBootstrapContext;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RestartApplicationListener}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
@ExtendWith(OutputCaptureExtension.class)
class RestartApplicationListenerTests {

	private static final String ENABLED_PROPERTY = "spring.devtools.restart.enabled";

	private static final String[] ARGS = new String[] { "a", "b", "c" };

	@BeforeEach
	@AfterEach
	void cleanup() {
		Restarter.clearInstance();
		System.clearProperty(ENABLED_PROPERTY);
	}

	@Test
	void isHighestPriority() {
		assertThat(new RestartApplicationListener().getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
	}

	@Test
	void initializeWithReady() {
		testInitialize(false, new ImplicitlyEnabledRestartApplicationListener());
		assertThat(Restarter.getInstance()).hasFieldOrPropertyWithValue("args", ARGS);
		assertThat(Restarter.getInstance().isFinished()).isTrue();
		assertThat((List<?>) ReflectionTestUtils.getField(Restarter.getInstance(), "rootContexts")).isNotEmpty();
	}

	@Test
	void initializeWithFail() {
		testInitialize(true, new ImplicitlyEnabledRestartApplicationListener());
		assertThat(Restarter.getInstance()).hasFieldOrPropertyWithValue("args", ARGS);
		assertThat(Restarter.getInstance().isFinished()).isTrue();
		assertThat((List<?>) ReflectionTestUtils.getField(Restarter.getInstance(), "rootContexts")).isEmpty();
	}

	@Test
	void disableWithSystemProperty(CapturedOutput output) {
		System.setProperty(ENABLED_PROPERTY, "false");
		testInitialize(false, new ImplicitlyEnabledRestartApplicationListener());
		assertThat(Restarter.getInstance()).hasFieldOrPropertyWithValue("enabled", false);
		assertThat(output).contains("Restart disabled due to System property");
	}

	@Test
	void enableWithSystemProperty(CapturedOutput output) {
		System.setProperty(ENABLED_PROPERTY, "true");
		testInitialize(false, new ImplicitlyEnabledRestartApplicationListener());
		assertThat(Restarter.getInstance()).hasFieldOrPropertyWithValue("enabled", true);
		assertThat(output).contains("Restart enabled irrespective of application packaging due to System property");
	}

	@Test
	void enableWithSystemPropertyWhenImplicitlyDisabled(CapturedOutput output) {
		System.setProperty(ENABLED_PROPERTY, "true");
		testInitialize(false, new RestartApplicationListener());
		assertThat(Restarter.getInstance()).hasFieldOrPropertyWithValue("enabled", true);
		assertThat(output).contains("Restart enabled irrespective of application packaging due to System property");
	}

	@Test
	void implicitlyDisabledInTests(CapturedOutput output) {
		testInitialize(false, new RestartApplicationListener());
		assertThat(Restarter.getInstance()).hasFieldOrPropertyWithValue("enabled", false);
		assertThat(output).contains("Restart disabled due to context in which it is running");
	}

	private void testInitialize(boolean failed, RestartApplicationListener listener) {
		Restarter.clearInstance();
		DefaultBootstrapContext bootstrapContext = new DefaultBootstrapContext();
		SpringApplication application = new SpringApplication();
		ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
		listener.onApplicationEvent(new ApplicationStartingEvent(bootstrapContext, application, ARGS));
		assertThat(Restarter.getInstance()).isNotNull();
		assertThat(Restarter.getInstance().isFinished()).isFalse();
		listener.onApplicationEvent(new ApplicationPreparedEvent(application, ARGS, context));
		if (failed) {
			listener.onApplicationEvent(new ApplicationFailedEvent(application, ARGS, context, new RuntimeException()));
		}
		else {
			listener.onApplicationEvent(new ApplicationReadyEvent(application, ARGS, context, null));
		}
	}

	private static final class ImplicitlyEnabledRestartApplicationListener extends RestartApplicationListener {

		@Override
		boolean implicitlyEnableRestart() {
			return true;
		}

	}

}

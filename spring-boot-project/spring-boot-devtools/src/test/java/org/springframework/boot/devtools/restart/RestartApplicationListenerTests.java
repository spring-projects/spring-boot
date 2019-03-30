/*
 * Copyright 2012-2018 the original author or authors.
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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RestartApplicationListener}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class RestartApplicationListenerTests {

	private static final String ENABLED_PROPERTY = "spring.devtools.restart.enabled";

	private static final String[] ARGS = new String[] { "a", "b", "c" };

	@Rule
	public final OutputCapture output = new OutputCapture();

	@Before
	@After
	public void cleanup() {
		Restarter.clearInstance();
		System.clearProperty(ENABLED_PROPERTY);
	}

	@Test
	public void isHighestPriority() {
		assertThat(new RestartApplicationListener().getOrder())
				.isEqualTo(Ordered.HIGHEST_PRECEDENCE);
	}

	@Test
	public void initializeWithReady() {
		testInitialize(false);
		assertThat(Restarter.getInstance()).hasFieldOrPropertyWithValue("args", ARGS);
		assertThat(Restarter.getInstance().isFinished()).isTrue();
		assertThat((List<?>) ReflectionTestUtils.getField(Restarter.getInstance(),
				"rootContexts")).isNotEmpty();
	}

	@Test
	public void initializeWithFail() {
		testInitialize(true);
		assertThat(Restarter.getInstance()).hasFieldOrPropertyWithValue("args", ARGS);
		assertThat(Restarter.getInstance().isFinished()).isTrue();
		assertThat((List<?>) ReflectionTestUtils.getField(Restarter.getInstance(),
				"rootContexts")).isEmpty();
	}

	@Test
	public void disableWithSystemProperty() {
		System.setProperty(ENABLED_PROPERTY, "false");
		this.output.reset();
		testInitialize(false);
		assertThat(Restarter.getInstance()).hasFieldOrPropertyWithValue("enabled", false);
		assertThat(this.output.toString())
				.contains("Restart disabled due to System property");
	}

	private void testInitialize(boolean failed) {
		Restarter.clearInstance();
		RestartApplicationListener listener = new RestartApplicationListener();
		SpringApplication application = new SpringApplication();
		ConfigurableApplicationContext context = mock(
				ConfigurableApplicationContext.class);
		listener.onApplicationEvent(new ApplicationStartingEvent(application, ARGS));
		assertThat(Restarter.getInstance()).isNotEqualTo(nullValue());
		assertThat(Restarter.getInstance().isFinished()).isFalse();
		listener.onApplicationEvent(
				new ApplicationPreparedEvent(application, ARGS, context));
		if (failed) {
			listener.onApplicationEvent(new ApplicationFailedEvent(application, ARGS,
					context, new RuntimeException()));
		}
		else {
			listener.onApplicationEvent(
					new ApplicationReadyEvent(application, ARGS, context));
		}
	}

}

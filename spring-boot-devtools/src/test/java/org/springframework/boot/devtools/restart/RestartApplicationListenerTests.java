/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.devtools.restart;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
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

	@Before
	@After
	public void cleanup() {
		Restarter.clearInstance();
		System.clearProperty(ENABLED_PROPERTY);
	}

	@Test
	public void isHighestPriority() throws Exception {
		assertThat(new RestartApplicationListener().getOrder())
				.isEqualTo(Ordered.HIGHEST_PRECEDENCE);
	}

	@Test
	public void initializeWithReady() throws Exception {
		testInitialize(false);
		assertThat(ReflectionTestUtils.getField(Restarter.getInstance(), "args"))
				.isEqualTo(ARGS);
		assertThat(Restarter.getInstance().isFinished()).isTrue();
		assertThat(ReflectionTestUtils.getField(Restarter.getInstance(), "rootContext"))
				.isNotNull();
	}

	@Test
	public void initializeWithFail() throws Exception {
		testInitialize(true);
		assertThat(ReflectionTestUtils.getField(Restarter.getInstance(), "args"))
				.isEqualTo(ARGS);
		assertThat(Restarter.getInstance().isFinished()).isTrue();
		assertThat(ReflectionTestUtils.getField(Restarter.getInstance(), "rootContext"))
				.isNull();
	}

	@Test
	public void disableWithSystemProperty() throws Exception {
		System.setProperty(ENABLED_PROPERTY, "false");
		testInitialize(false);
		assertThat(ReflectionTestUtils.getField(Restarter.getInstance(), "enabled"))
				.isEqualTo(false);
	}

	private void testInitialize(boolean failed) {
		Restarter.clearInstance();
		RestartApplicationListener listener = new RestartApplicationListener();
		SpringApplication application = new SpringApplication();
		ConfigurableApplicationContext context = mock(
				ConfigurableApplicationContext.class);
		listener.onApplicationEvent(new ApplicationStartedEvent(application, ARGS));
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

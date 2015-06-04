/*
 * Copyright 2012-2015 the original author or authors.
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
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.devtools.restart.RestartApplicationListener;
import org.springframework.boot.devtools.restart.Restarter;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.test.util.ReflectionTestUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RestartApplicationListener}.
 *
 * @author Phillip Webb
 */
public class RestartApplicationListenerTests {

	@Before
	@After
	public void cleanup() {
		Restarter.clearInstance();
	}

	@Test
	public void isHighestPriority() throws Exception {
		assertThat(new RestartApplicationListener().getOrder(),
				equalTo(Ordered.HIGHEST_PRECEDENCE));
	}

	@Test
	public void initializeWithReady() throws Exception {
		testInitialize(false);
	}

	@Test
	public void initializeWithFail() throws Exception {
		testInitialize(true);
	}

	private void testInitialize(boolean failed) {
		Restarter.clearInstance();
		RestartApplicationListener listener = new RestartApplicationListener();
		SpringApplication application = new SpringApplication();
		ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
		String[] args = new String[] { "a", "b", "c" };
		listener.onApplicationEvent(new ApplicationStartedEvent(application, args));
		assertThat(Restarter.getInstance(), not(nullValue()));
		assertThat(Restarter.getInstance().isFinished(), equalTo(false));
		assertThat(ReflectionTestUtils.getField(Restarter.getInstance(), "args"),
				equalTo((Object) args));
		if (failed) {
			listener.onApplicationEvent(new ApplicationFailedEvent(application, args,
					context, new RuntimeException()));
		}
		else {
			listener.onApplicationEvent(new ApplicationReadyEvent(application, args,
					context));
		}
		assertThat(Restarter.getInstance().isFinished(), equalTo(true));
	}

}

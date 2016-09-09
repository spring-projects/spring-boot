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

import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link OnInitializedRestarterCondition}.
 *
 * @author Phillip Webb
 */
public class OnInitializedRestarterConditionTests {

	private static Object wait = new Object();

	@Before
	@After
	public void cleanup() {
		Restarter.clearInstance();
	}

	@Test
	public void noInstance() throws Exception {
		Restarter.clearInstance();
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(
				Config.class);
		assertThat(context.containsBean("bean"), equalTo(false));
		context.close();
	}

	@Test
	public void noInitialization() throws Exception {
		Restarter.initialize(new String[0], false, RestartInitializer.NONE);
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(
				Config.class);
		assertThat(context.containsBean("bean"), equalTo(false));
		context.close();
	}

	@Test
	public void initialized() throws Exception {
		Thread thread = new Thread() {

			@Override
			public void run() {
				TestInitialized.main();
			};

		};
		thread.start();
		synchronized (wait) {
			wait.wait();
		}
	}

	public static class TestInitialized {

		public static void main(String... args) {
			RestartInitializer initializer = mock(RestartInitializer.class);
			given(initializer.getInitialUrls((Thread) any())).willReturn(new URL[0]);
			Restarter.initialize(new String[0], false, initializer);
			ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(
					Config.class);
			assertThat(context.containsBean("bean"), equalTo(true));
			context.close();
			synchronized (wait) {
				wait.notify();
			}
		}

	}

	@Configuration
	public static class Config {

		@Bean
		@ConditionalOnInitializedRestarter
		public String bean() {
			return "bean";
		}

	}

}

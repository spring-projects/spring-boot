/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot;

import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.Banner.Mode;
import org.springframework.boot.testutil.InternalOutputCapture;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link Banner} and its usage by {@link SpringApplication}.
 *
 * @author Phillip Webb
 * @author Michael Stummvoll
 * @author Michael Simons
 */
public class BannerTests {

	private ConfigurableApplicationContext context;

	@After
	public void cleanUp() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Rule
	public InternalOutputCapture out = new InternalOutputCapture();

	@Captor
	private ArgumentCaptor<Class<?>> sourceClassCaptor;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testDefaultBanner() throws Exception {
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebEnvironment(false);
		this.context = application.run();
		assertThat(this.out.toString()).contains(":: Spring Boot ::");
	}

	@Test
	public void testDefaultBannerInLog() throws Exception {
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebEnvironment(false);
		this.context = application.run();
		assertThat(this.out.toString()).contains(":: Spring Boot ::");
	}

	@Test
	public void testCustomBanner() throws Exception {
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebEnvironment(false);
		application.setBanner(new DummyBanner());
		this.context = application.run();
		assertThat(this.out.toString()).contains("My Banner");
	}

	@Test
	public void testBannerInContext() throws Exception {
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebEnvironment(false);
		this.context = application.run();
		assertThat(this.context.containsBean("springBootBanner")).isTrue();
	}

	@Test
	public void testCustomBannerInContext() throws Exception {
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebEnvironment(false);
		Banner banner = mock(Banner.class);
		application.setBanner(banner);
		this.context = application.run();
		Banner printedBanner = (Banner) this.context.getBean("springBootBanner");
		assertThat(ReflectionTestUtils.getField(printedBanner, "banner"))
				.isEqualTo(banner);
		verify(banner).printBanner(any(Environment.class),
				this.sourceClassCaptor.capture(), any(PrintStream.class));
		reset(banner);
		printedBanner.printBanner(this.context.getEnvironment(), null, System.out);
		verify(banner).printBanner(any(Environment.class),
				eq(this.sourceClassCaptor.getValue()), any(PrintStream.class));
	}

	@Test
	public void testDisableBannerInContext() throws Exception {
		SpringApplication application = new SpringApplication(Config.class);
		application.setBannerMode(Mode.OFF);
		application.setWebEnvironment(false);
		this.context = application.run();
		assertThat(this.context.containsBean("springBootBanner")).isFalse();
	}

	static class DummyBanner implements Banner {

		@Override
		public void printBanner(Environment environment, Class<?> sourceClass,
				PrintStream out) {
			out.println("My Banner");
		}

	}

	@Configuration
	public static class Config {

	}

}

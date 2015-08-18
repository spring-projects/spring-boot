/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot;

import java.io.PrintStream;

import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.OutputCapture;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link Banner} and its usage by {@link SpringApplication}.
 *
 * @author Phillip Webb
 * @author Michael Stummvoll
 */
public class BannerTests {

	@Rule
	public OutputCapture out = new OutputCapture();

	@Test
	public void testDefaultBanner() throws Exception {
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebEnvironment(false);
		application.run();
		assertThat(this.out.toString(), containsString(":: Spring Boot ::"));
	}

	@Test
	public void testCustomBanner() throws Exception {
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebEnvironment(false);
		application.setBanner(new DummyBanner());
		application.run();
		assertThat(this.out.toString(), containsString("My Banner"));
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

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

package org.springframework.boot.autoconfigure.condition;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link ResourceCondition}.
 *
 * @author Stephane Nicoll
 */
public class ResourceConditionTests {

	private ConfigurableApplicationContext context;

	@After
	public void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void defaultResourceAndNoExplicitKey() {
		load(DefaultLocationConfiguration.class);
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	@Test
	public void unknownDefaultLocationAndNoExplicitKey() {
		load(UnknownDefaultLocationConfiguration.class);
		assertThat(this.context.containsBean("foo")).isFalse();
	}

	@Test
	public void unknownDefaultLocationAndExplicitKeyToResource() {
		load(UnknownDefaultLocationConfiguration.class,
				"spring.foo.test.config=logging.properties");
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	private void load(Class<?> config, String... environment) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(applicationContext, environment);
		applicationContext.register(config);
		applicationContext.refresh();
		this.context = applicationContext;
	}

	@Configuration
	@Conditional(DefaultLocationResourceCondition.class)
	static class DefaultLocationConfiguration {

		@Bean
		public String foo() {
			return "foo";
		}
	}

	@Configuration
	@Conditional(UnknownDefaultLocationResourceCondition.class)
	static class UnknownDefaultLocationConfiguration {

		@Bean
		public String foo() {
			return "foo";
		}
	}

	private static class DefaultLocationResourceCondition extends ResourceCondition {

		DefaultLocationResourceCondition() {
			super("test", "spring.foo.test.", "config", "classpath:/logging.properties");
		}
	}

	private static class UnknownDefaultLocationResourceCondition
			extends ResourceCondition {

		UnknownDefaultLocationResourceCondition() {
			super("test", "spring.foo.test", "config",
					"classpath:/this-file-does-not-exist.xml");
		}

	}
}

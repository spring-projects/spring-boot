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

package org.springframework.boot.autoconfigure.condition;

import org.junit.Test;

import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link AllNestedConditions}.
 */
public class AllNestedConditionsTests {

	@Test
	public void neither() throws Exception {
		AnnotationConfigApplicationContext context = load(Config.class);
		assertThat(context.containsBean("myBean"), equalTo(false));
		context.close();
	}

	@Test
	public void propertyA() throws Exception {
		AnnotationConfigApplicationContext context = load(Config.class, "a:a");
		assertThat(context.containsBean("myBean"), equalTo(false));
		context.close();
	}

	@Test
	public void propertyB() throws Exception {
		AnnotationConfigApplicationContext context = load(Config.class, "b:b");
		assertThat(context.containsBean("myBean"), equalTo(false));
		context.close();
	}

	@Test
	public void both() throws Exception {
		AnnotationConfigApplicationContext context = load(Config.class, "a:a", "b:b");
		assertThat(context.containsBean("myBean"), equalTo(true));
		context.close();
	}

	private AnnotationConfigApplicationContext load(Class<?> config, String... env) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(context, env);
		context.register(config);
		context.refresh();
		return context;
	}

	@Configuration
	@Conditional(OnPropertyAAndBCondition.class)
	public static class Config {

		@Bean
		public String myBean() {
			return "myBean";
		}

	}

	static class OnPropertyAAndBCondition extends AllNestedConditions {

		OnPropertyAAndBCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@ConditionalOnProperty("a")
		static class HasPropertyA {

		}

		@ConditionalOnProperty("b")
		static class HasPropertyB {

		}

	}

}

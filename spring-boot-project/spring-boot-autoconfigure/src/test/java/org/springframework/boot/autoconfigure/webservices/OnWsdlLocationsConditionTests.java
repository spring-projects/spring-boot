/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.webservices;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OnWsdlLocationsCondition}.
 *
 * @author Eneias Silva
 */
public class OnWsdlLocationsConditionTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void wsdlLocationsNotDefined() {
		load(TestConfig.class);
		assertThat(this.context.containsBean("foo")).isFalse();
	}

	@Test
	public void wsdlLocationsDefinedAsCommaSeparated() {
		load(TestConfig.class, "spring.webservices.wsdl-locations=value1");
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	@Test
	public void wsdlLocationsDefinedAsList() {
		load(TestConfig.class, "spring.webservices.wsdl-locations[0]=value1");
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	private void load(Class<?> config, String... environment) {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of(environment).applyTo(this.context);
		this.context.register(config);
		this.context.refresh();
	}

	@Configuration
	@Conditional(OnWsdlLocationsCondition.class)
	protected static class TestConfig {

		@Bean
		public String foo() {
			return "foo";
		}

	}

}

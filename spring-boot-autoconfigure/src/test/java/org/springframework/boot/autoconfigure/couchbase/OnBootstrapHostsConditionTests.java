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

package org.springframework.boot.autoconfigure.couchbase;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OnBootstrapHostsCondition}.
 *
 * @author Stephane Nicoll
 */
public class OnBootstrapHostsConditionTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void bootstrapHostsNotDefined() {
		load(TestConfig.class);
		assertThat(this.context.containsBean("foo")).isFalse();
	}

	@Test
	public void bootstrapHostsDefinedAsCommaSeparated() {
		load(TestConfig.class, "spring.couchbase.bootstrap-hosts=value1");
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	@Test
	public void bootstrapHostsDefinedAsList() {
		load(TestConfig.class, "spring.couchbase.bootstrap-hosts[0]=value1");
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	@Test
	public void bootstrapHostsDefinedAsCommaSeparatedRelaxed() {
		load(TestConfig.class, "spring.couchbase.bootstrapHosts=value1");
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	@Test
	public void bootstrapHostsDefinedAsListRelaxed() {
		load(TestConfig.class, "spring.couchbase.bootstrapHosts[0]=value1");
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	private void load(Class<?> config, String... environment) {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context, environment);
		this.context.register(config);
		this.context.refresh();
	}

	@Configuration
	@Conditional(OnBootstrapHostsCondition.class)
	protected static class TestConfig {

		@Bean
		public String foo() {
			return "foo";
		}

	}

}

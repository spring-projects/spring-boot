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

package org.springframework.boot.neo4j;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NodeEntityScan}.
 *
 * @author Stephane Nicoll
 */
public class NodeEntityScanTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private AnnotationConfigApplicationContext context;

	@After
	public void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void simpleValue() throws Exception {
		this.context = new AnnotationConfigApplicationContext(ValueConfig.class);
		assertSetPackagesToScan("com.mycorp.entity");
	}

	@Test
	public void needsSessionFactoryFactory() throws Exception {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Unable to configure "
				+ "SessionFactoryFactoryBean from @NodeEntityScan, "
				+ "ensure an appropriate bean is registered.");
		this.context = new AnnotationConfigApplicationContext(
				MissingSessionFactory.class);
	}

	private void assertSetPackagesToScan(String... expected) {
		String[] actual = this.context.getBean(TestSessionFactoryProvider.class)
				.getPackagesToScan();
		assertThat(actual).isEqualTo(expected);
	}

	@Configuration
	static class BaseConfig {

		@Bean
		public SessionFactoryProvider sessionFactoryFactoryBean() {
			return new TestSessionFactoryProvider();
		}

	}

	@NodeEntityScan("com.mycorp.entity")
	static class ValueConfig extends BaseConfig {
	}

	@Configuration
	@NodeEntityScan("com.mycorp.entity")
	static class MissingSessionFactory {
	}

	private static class TestSessionFactoryProvider extends SessionFactoryProvider {

		private String[] packagesToScan;

		@Override
		public void setPackagesToScan(String... packagesToScan) {
			this.packagesToScan = packagesToScan;
		}

		public String[] getPackagesToScan() {
			return this.packagesToScan;
		}

	}

}

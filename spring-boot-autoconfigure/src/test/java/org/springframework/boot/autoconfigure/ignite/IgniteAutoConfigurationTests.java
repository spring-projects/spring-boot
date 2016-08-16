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

package org.springframework.boot.autoconfigure.ignite;

import java.io.IOException;

import org.apache.ignite.Ignite;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.logger.slf4j.Slf4jLogger;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link IgniteAutoConfiguration}
 *
 * @author wmz7year
 */
public class IgniteAutoConfigurationTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private AnnotationConfigApplicationContext context;

	@After
	public void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void explicitConfigFile() throws IOException {
		load("spring.ignite.config=org/springframework/boot/autoconfigure/ignite/"
				+ "ignite-specific.xml");
		Ignite igniteInstance = this.context.getBean(Ignite.class);
		assertThat(igniteInstance.configuration().getGridName())
				.isEqualTo("testMainGridName");
	}

	@Test
	public void unknownConfigFile() {
		this.thrown.expect(BeanCreationException.class);
		this.thrown.expectMessage("foo/bar/unknown.xml");
		load("spring.ignite.config=foo/bar/unknown.xml");
	}

	private void load(String... environment) {
		load(null, environment);
	}

	private void load(Class<?> config, String... environment) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(applicationContext, environment);
		if (config != null) {
			applicationContext.register(config);
		}
		applicationContext.register(IgniteAutoConfiguration.class);
		applicationContext.refresh();
		this.context = applicationContext;
	}

	@Configuration
	static class IgniteConfigWithName {

		@Bean
		public IgniteConfiguration myIgniteConfig() {
			IgniteConfiguration igniteConfiguration = new IgniteConfiguration();
			igniteConfiguration.setGridName("my-test-grid");
			igniteConfiguration.setGridLogger(new Slf4jLogger());
			return igniteConfiguration;
		}
	}
}

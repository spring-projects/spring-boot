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

package org.springframework.boot.autoconfigure.hazelcast;

import java.io.IOException;
import java.util.Map;

import com.hazelcast.config.Config;
import com.hazelcast.config.QueueConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link HazelcastAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
public class HazelcastAutoConfigurationTests {

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
	public void defaultConfigFile() throws IOException {
		load(); // hazelcast.xml present in root classpath
		HazelcastInstance hazelcastInstance = this.context
				.getBean(HazelcastInstance.class);
		assertThat(hazelcastInstance.getConfig().getConfigurationUrl(),
				equalTo(new ClassPathResource("hazelcast.xml").getURL()));
	}

	@Test
	public void systemProperty() throws IOException {
		System.setProperty(HazelcastConfigResourceCondition.CONFIG_SYSTEM_PROPERTY,
				"classpath:org/springframework/boot/autoconfigure/hazelcast/hazelcast-specific.xml");
		try {
			load();
			HazelcastInstance hazelcastInstance = this.context
					.getBean(HazelcastInstance.class);
			Map<String, QueueConfig> queueConfigs = hazelcastInstance.getConfig()
					.getQueueConfigs();
			assertThat(queueConfigs.values(), hasSize(1));
			assertThat(queueConfigs, hasKey("foobar"));
		}
		finally {
			System.clearProperty(HazelcastConfigResourceCondition.CONFIG_SYSTEM_PROPERTY);
		}
	}

	@Test
	public void explicitConfigFile() throws IOException {
		load("spring.hazelcast.config=org/springframework/boot/autoconfigure/hazelcast/"
				+ "hazelcast-specific.xml");
		HazelcastInstance hazelcastInstance = this.context
				.getBean(HazelcastInstance.class);
		assertThat(hazelcastInstance.getConfig().getConfigurationFile(),
				equalTo(new ClassPathResource(
						"org/springframework/boot/autoconfigure/hazelcast"
								+ "/hazelcast-specific.xml").getFile()));
	}

	@Test
	public void explicitConfigUrl() throws IOException {
		load("spring.hazelcast.config=hazelcast-default.xml");
		HazelcastInstance hazelcastInstance = this.context
				.getBean(HazelcastInstance.class);
		assertThat(hazelcastInstance.getConfig().getConfigurationUrl(),
				equalTo(new ClassPathResource("hazelcast-default.xml").getURL()));
	}

	@Test
	public void unknownConfigFile() {
		this.thrown.expect(BeanCreationException.class);
		this.thrown.expectMessage("foo/bar/unknown.xml");
		load("spring.hazelcast.config=foo/bar/unknown.xml");
	}

	@Test
	public void configInstanceWithName() {
		Config config = new Config("my-test-instance");
		HazelcastInstance existingHazelcastInstance = Hazelcast
				.newHazelcastInstance(config);
		try {
			load(HazelcastConfigWithName.class,
					"spring.hazelcast.config=this-is-ignored.xml");
			HazelcastInstance hazelcastInstance = this.context
					.getBean(HazelcastInstance.class);
			assertThat(hazelcastInstance.getConfig().getInstanceName(),
					equalTo("my-test-instance"));
			// Should reuse any existing instance by default.
			assertThat(hazelcastInstance, equalTo(existingHazelcastInstance));
		}
		finally {
			existingHazelcastInstance.shutdown();
		}
	}

	@Test
	public void configInstanceWithoutName() {
		load(HazelcastConfigNoName.class, "spring.hazelcast.config=this-is-ignored.xml");
		HazelcastInstance hazelcastInstance = this.context
				.getBean(HazelcastInstance.class);
		Map<String, QueueConfig> queueConfigs = hazelcastInstance.getConfig()
				.getQueueConfigs();
		assertThat(queueConfigs.values(), hasSize(1));
		assertThat(queueConfigs, hasKey("another-queue"));
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
		applicationContext.register(HazelcastAutoConfiguration.class);
		applicationContext.refresh();
		this.context = applicationContext;
	}

	@Configuration
	static class HazelcastConfigWithName {

		@Bean
		public Config myHazelcastConfig() {
			return new Config("my-test-instance");
		}

	}

	@Configuration
	static class HazelcastConfigNoName {

		@Bean
		public Config anotherHazelcastConfig() {
			Config config = new Config();
			config.addQueueConfig(new QueueConfig("another-queue"));
			return config;
		}

	}

}

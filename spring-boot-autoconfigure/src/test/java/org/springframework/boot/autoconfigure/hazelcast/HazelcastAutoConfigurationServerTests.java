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

package org.springframework.boot.autoconfigure.hazelcast;

import java.io.IOException;
import java.util.Map;

import com.hazelcast.config.Config;
import com.hazelcast.config.QueueConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.test.context.ContextLoader;
import org.springframework.boot.testsupport.runner.classpath.ClassPathExclusions;
import org.springframework.boot.testsupport.runner.classpath.ModifiedClassPathRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HazelcastAutoConfiguration} when the client library is not present.
 *
 * @author Stephane Nicoll
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions("hazelcast-client-*.jar")
public class HazelcastAutoConfigurationServerTests {

	private final ContextLoader<AnnotationConfigApplicationContext> contextLoader = ContextLoader
			.standard().autoConfig(HazelcastAutoConfiguration.class);

	@Test
	public void defaultConfigFile() throws IOException {
		// hazelcast.xml present in root classpath
		this.contextLoader.load(context -> {
			HazelcastInstance hazelcastInstance = context
					.getBean(HazelcastInstance.class);
			assertThat(hazelcastInstance.getConfig().getConfigurationUrl())
					.isEqualTo(new ClassPathResource("hazelcast.xml").getURL());
		});
	}

	@Test
	public void systemProperty() throws IOException {
		this.contextLoader
				.systemProperty(HazelcastServerConfiguration.CONFIG_SYSTEM_PROPERTY,
						"classpath:org/springframework/boot/autoconfigure/hazelcast/hazelcast-specific.xml")
				.load(context -> {

					HazelcastInstance hazelcastInstance = context
							.getBean(HazelcastInstance.class);
					Map<String, QueueConfig> queueConfigs = hazelcastInstance.getConfig()
							.getQueueConfigs();
					assertThat(queueConfigs).hasSize(1).containsKey("foobar");
				});
	}

	@Test
	public void explicitConfigFile() throws IOException {
		this.contextLoader
				.env("spring.hazelcast.config=org/springframework/boot/autoconfigure/hazelcast/"
						+ "hazelcast-specific.xml")
				.load(context -> {
					HazelcastInstance hazelcastInstance = context
							.getBean(HazelcastInstance.class);
					assertThat(hazelcastInstance.getConfig().getConfigurationFile())
							.isEqualTo(new ClassPathResource(
									"org/springframework/boot/autoconfigure/hazelcast"
											+ "/hazelcast-specific.xml").getFile());
				});
	}

	@Test
	public void explicitConfigUrl() throws IOException {
		this.contextLoader.env("spring.hazelcast.config=hazelcast-default.xml")
				.load(context -> {
					HazelcastInstance hazelcastInstance = context
							.getBean(HazelcastInstance.class);
					assertThat(hazelcastInstance.getConfig().getConfigurationUrl())
							.isEqualTo(new ClassPathResource("hazelcast-default.xml")
									.getURL());
				});
	}

	@Test
	public void unknownConfigFile() {
		this.contextLoader.env("spring.hazelcast.config=foo/bar/unknown.xml").loadAndFail(
				BeanCreationException.class,
				ex -> assertThat(ex.getMessage()).contains("foo/bar/unknown.xml"));
	}

	@Test
	public void configInstanceWithName() {
		Config config = new Config("my-test-instance");
		HazelcastInstance existingHazelcastInstance = Hazelcast
				.newHazelcastInstance(config);
		try {
			this.contextLoader.config(HazelcastConfigWithName.class)
					.env("spring.hazelcast.config=this-is-ignored.xml").load(context -> {
						HazelcastInstance hazelcastInstance = context
								.getBean(HazelcastInstance.class);
						assertThat(hazelcastInstance.getConfig().getInstanceName())
								.isEqualTo("my-test-instance");
						// Should reuse any existing instance by default.
						assertThat(hazelcastInstance)
								.isEqualTo(existingHazelcastInstance);
					});
		}
		finally {
			existingHazelcastInstance.shutdown();
		}
	}

	@Test
	public void configInstanceWithoutName() {
		this.contextLoader.config(HazelcastConfigNoName.class)
				.env("spring.hazelcast.config=this-is-ignored.xml").load(context -> {
					HazelcastInstance hazelcastInstance = context
							.getBean(HazelcastInstance.class);
					Map<String, QueueConfig> queueConfigs = hazelcastInstance.getConfig()
							.getQueueConfigs();
					assertThat(queueConfigs).hasSize(1).containsKey("another-queue");
				});
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

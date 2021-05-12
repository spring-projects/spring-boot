/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.hazelcast;

import java.util.Map;

import com.hazelcast.config.Config;
import com.hazelcast.config.QueueConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HazelcastAutoConfiguration} when the client library is not present.
 *
 * @author Stephane Nicoll
 */
@ClassPathExclusions("hazelcast-client-*.jar")
class HazelcastAutoConfigurationServerTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(HazelcastAutoConfiguration.class));

	@Test
	void defaultConfigFile() {
		// hazelcast.xml present in root classpath
		this.contextRunner.run((context) -> {
			Config config = context.getBean(HazelcastInstance.class).getConfig();
			assertThat(config.getConfigurationUrl()).isEqualTo(new ClassPathResource("hazelcast.xml").getURL());
		});
	}

	@Test
	void systemPropertyWithXml() {
		this.contextRunner
				.withSystemProperties(HazelcastServerConfiguration.CONFIG_SYSTEM_PROPERTY
						+ "=classpath:org/springframework/boot/autoconfigure/hazelcast/hazelcast-specific.xml")
				.run((context) -> {
					Config config = context.getBean(HazelcastInstance.class).getConfig();
					assertThat(config.getMapConfigs().keySet()).containsOnly("foobar");
				});
	}

	@Test
	void systemPropertyWithYaml() {
		this.contextRunner
				.withSystemProperties(HazelcastServerConfiguration.CONFIG_SYSTEM_PROPERTY
						+ "=classpath:org/springframework/boot/autoconfigure/hazelcast/hazelcast-specific.yaml")
				.run((context) -> {
					Config config = context.getBean(HazelcastInstance.class).getConfig();
					assertThat(config.getMapConfigs().keySet()).containsOnly("foobar");
				});
	}

	@Test
	void explicitConfigFileWithXml() {
		this.contextRunner
				.withPropertyValues("spring.hazelcast.config=org/springframework/boot/autoconfigure/hazelcast/"
						+ "hazelcast-specific.xml")
				.run(assertSpecificHazelcastServer(
						"org/springframework/boot/autoconfigure/hazelcast/hazelcast-specific.xml"));
	}

	@Test
	void explicitConfigFileWithYaml() {
		this.contextRunner
				.withPropertyValues("spring.hazelcast.config=org/springframework/boot/autoconfigure/hazelcast/"
						+ "hazelcast-specific.yaml")
				.run(assertSpecificHazelcastServer(
						"org/springframework/boot/autoconfigure/hazelcast/hazelcast-specific.yaml"));
	}

	@Test
	void explicitConfigUrlWithXml() {
		this.contextRunner
				.withPropertyValues("spring.hazelcast.config=classpath:org/springframework/"
						+ "boot/autoconfigure/hazelcast/hazelcast-specific.xml")
				.run(assertSpecificHazelcastServer(
						"org/springframework/boot/autoconfigure/hazelcast/hazelcast-specific.xml"));
	}

	@Test
	void explicitConfigUrlWithYaml() {
		this.contextRunner
				.withPropertyValues("spring.hazelcast.config=classpath:org/springframework/"
						+ "boot/autoconfigure/hazelcast/hazelcast-specific.yaml")
				.run(assertSpecificHazelcastServer(
						"org/springframework/boot/autoconfigure/hazelcast/hazelcast-specific.yaml"));
	}

	private ContextConsumer<AssertableApplicationContext> assertSpecificHazelcastServer(String location) {
		return (context) -> {
			Config config = context.getBean(HazelcastInstance.class).getConfig();
			assertThat(config.getConfigurationUrl()).asString().endsWith(location);
		};
	}

	@Test
	void unknownConfigFile() {
		this.contextRunner.withPropertyValues("spring.hazelcast.config=foo/bar/unknown.xml")
				.run((context) -> assertThat(context).getFailure().isInstanceOf(BeanCreationException.class)
						.hasMessageContaining("foo/bar/unknown.xml"));
	}

	@Test
	void configInstanceWithName() {
		Config config = new Config("my-test-instance");
		HazelcastInstance existing = Hazelcast.newHazelcastInstance(config);
		try {
			this.contextRunner.withUserConfiguration(HazelcastConfigWithName.class)
					.withPropertyValues("spring.hazelcast.config=this-is-ignored.xml").run((context) -> {
						HazelcastInstance hazelcast = context.getBean(HazelcastInstance.class);
						assertThat(hazelcast.getConfig().getInstanceName()).isEqualTo("my-test-instance");
						// Should reuse any existing instance by default.
						assertThat(hazelcast).isEqualTo(existing);
					});
		}
		finally {
			existing.shutdown();
		}
	}

	@Test
	void configInstanceWithoutName() {
		this.contextRunner.withUserConfiguration(HazelcastConfigNoName.class)
				.withPropertyValues("spring.hazelcast.config=this-is-ignored.xml").run((context) -> {
					Config config = context.getBean(HazelcastInstance.class).getConfig();
					Map<String, QueueConfig> queueConfigs = config.getQueueConfigs();
					assertThat(queueConfigs.keySet()).containsOnly("another-queue");
				});
	}

	@Test
	void autoConfiguredConfigUsesApplicationClassLoader() {
		this.contextRunner.run((context) -> {
			Config config = context.getBean(HazelcastInstance.class).getConfig();
			assertThat(config.getClassLoader()).isSameAs(context.getSourceApplicationContext().getClassLoader());
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class HazelcastConfigWithName {

		@Bean
		Config myHazelcastConfig() {
			return new Config("my-test-instance");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class HazelcastConfigNoName {

		@Bean
		Config anotherHazelcastConfig() {
			Config config = new Config();
			config.addQueueConfig(new QueueConfig("another-queue"));
			return config;
		}

	}

}

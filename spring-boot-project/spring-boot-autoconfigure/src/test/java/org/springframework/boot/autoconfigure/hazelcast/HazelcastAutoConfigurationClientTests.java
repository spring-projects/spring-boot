/*
 * Copyright 2012-2019 the original author or authors.
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

import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.impl.clientside.HazelcastClientProxy;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HazelcastAutoConfiguration} specific to the client.
 *
 * @author Vedran Pavic
 * @author Stephane Nicoll
 */
class HazelcastAutoConfigurationClientTests {

	/**
	 * Servers the test clients will connect to.
	 */
	private static HazelcastInstance hazelcastServer;

	@BeforeAll
	static void init() {
		hazelcastServer = Hazelcast.newHazelcastInstance();
	}

	@AfterAll
	static void close() {
		if (hazelcastServer != null) {
			hazelcastServer.shutdown();
		}
	}

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(HazelcastAutoConfiguration.class));

	@Test
	void systemPropertyWithXml() {
		this.contextRunner
				.withSystemProperties(HazelcastClientConfiguration.CONFIG_SYSTEM_PROPERTY
						+ "=classpath:org/springframework/boot/autoconfigure/hazelcast/hazelcast-client-specific.xml")
				.run(assertSpecificHazelcastClient("explicit-xml"));
	}

	@Test
	void systemPropertyWithYaml() {
		this.contextRunner
				.withSystemProperties(HazelcastClientConfiguration.CONFIG_SYSTEM_PROPERTY
						+ "=classpath:org/springframework/boot/autoconfigure/hazelcast/hazelcast-client-specific.yaml")
				.run(assertSpecificHazelcastClient("explicit-yaml"));
	}

	@Test
	void explicitConfigFileWithXml() {
		this.contextRunner.withPropertyValues("spring.hazelcast.config=org/springframework/boot/autoconfigure/"
				+ "hazelcast/hazelcast-client-specific.xml").run(assertSpecificHazelcastClient("explicit-xml"));
	}

	@Test
	void explicitConfigFileWithYaml() {
		this.contextRunner
				.withPropertyValues("spring.hazelcast.config=org/springframework/boot/autoconfigure/"
						+ "hazelcast/hazelcast-client-specific.yaml")
				.run(assertSpecificHazelcastClient("explicit-yaml"));
	}

	@Test
	void explicitConfigUrlWithXml() {
		this.contextRunner
				.withPropertyValues("spring.hazelcast.config=classpath:org/springframework/"
						+ "boot/autoconfigure/hazelcast/hazelcast-client-specific.xml")
				.run(assertSpecificHazelcastClient("explicit-xml"));
	}

	@Test
	void explicitConfigUrlWithYaml() {
		this.contextRunner
				.withPropertyValues("spring.hazelcast.config=classpath:org/springframework/"
						+ "boot/autoconfigure/hazelcast/hazelcast-client-specific.yaml")
				.run(assertSpecificHazelcastClient("explicit-yaml"));
	}

	@Test
	void unknownConfigFile() {
		this.contextRunner.withPropertyValues("spring.hazelcast.config=foo/bar/unknown.xml")
				.run((context) -> assertThat(context).getFailure().isInstanceOf(BeanCreationException.class)
						.hasMessageContaining("foo/bar/unknown.xml"));
	}

	@Test
	void clientConfigTakesPrecedence() {
		this.contextRunner.withUserConfiguration(HazelcastServerAndClientConfig.class)
				.withPropertyValues("spring.hazelcast.config=this-is-ignored.xml").run((context) -> assertThat(context)
						.getBean(HazelcastInstance.class).isInstanceOf(HazelcastClientProxy.class));
	}

	private ContextConsumer<AssertableApplicationContext> assertSpecificHazelcastClient(String label) {
		return (context) -> assertThat(context).getBean(HazelcastInstance.class).isInstanceOf(HazelcastInstance.class)
				.has(labelEqualTo(label));
	}

	private static Condition<HazelcastInstance> labelEqualTo(String label) {
		return new Condition<>((o) -> ((HazelcastClientProxy) o).getClientConfig().getLabels().stream()
				.anyMatch((e) -> e.equals(label)), "Label equals to " + label);
	}

	@Configuration(proxyBeanMethods = false)
	static class HazelcastServerAndClientConfig {

		@Bean
		Config config() {
			return new Config();
		}

		@Bean
		ClientConfig clientConfig() {
			return new ClientConfig();
		}

	}

}

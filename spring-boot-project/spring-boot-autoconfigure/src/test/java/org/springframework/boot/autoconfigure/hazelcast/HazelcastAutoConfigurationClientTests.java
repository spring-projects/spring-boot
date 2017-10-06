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

import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.impl.HazelcastClientProxy;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.assertj.core.api.Condition;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HazelcastAutoConfiguration} specific to the client.
 *
 * @author Vedran Pavic
 * @author Stephane Nicoll
 */
public class HazelcastAutoConfigurationClientTests {

	/**
	 * Servers the test clients will connect to.
	 */
	private static HazelcastInstance hazelcastServer;

	@BeforeClass
	public static void init() {
		hazelcastServer = Hazelcast.newHazelcastInstance();
	}

	@AfterClass
	public static void close() {
		if (hazelcastServer != null) {
			hazelcastServer.shutdown();
		}
	}

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(HazelcastAutoConfiguration.class));

	@Test
	public void systemProperty() throws IOException {
		this.contextRunner
				.withSystemProperties(HazelcastClientConfiguration.CONFIG_SYSTEM_PROPERTY
						+ "=classpath:org/springframework/boot/autoconfigure/hazelcast/"
						+ "hazelcast-client-specific.xml")
				.run((context) -> assertThat(context).getBean(HazelcastInstance.class)
						.isInstanceOf(HazelcastInstance.class)
						.has(nameStartingWith("hz.client_")));
	}

	@Test
	public void explicitConfigFile() throws IOException {
		this.contextRunner
				.withPropertyValues(
						"spring.hazelcast.config=org/springframework/boot/autoconfigure/"
								+ "hazelcast/hazelcast-client-specific.xml")
				.run((context) -> assertThat(context).getBean(HazelcastInstance.class)
						.isInstanceOf(HazelcastClientProxy.class)
						.has(nameStartingWith("hz.client_")));
	}

	@Test
	public void explicitConfigUrl() throws IOException {
		this.contextRunner
				.withPropertyValues(
						"spring.hazelcast.config=hazelcast-client-default.xml")
				.run((context) -> assertThat(context).getBean(HazelcastInstance.class)
						.isInstanceOf(HazelcastClientProxy.class)
						.has(nameStartingWith("hz.client_")));
	}

	@Test
	public void unknownConfigFile() {
		this.contextRunner
				.withPropertyValues("spring.hazelcast.config=foo/bar/unknown.xml")
				.run((context) -> assertThat(context).getFailure()
						.isInstanceOf(BeanCreationException.class)
						.hasMessageContaining("foo/bar/unknown.xml"));
	}

	@Test
	public void clientConfigTakesPrecedence() {
		this.contextRunner.withUserConfiguration(HazelcastServerAndClientConfig.class)
				.withPropertyValues("spring.hazelcast.config=this-is-ignored.xml")
				.run((context) -> assertThat(context).getBean(HazelcastInstance.class)
						.isInstanceOf(HazelcastClientProxy.class));
	}

	private Condition<HazelcastInstance> nameStartingWith(String prefix) {
		return new Condition<>((o) -> o.getName().startsWith(prefix),
				"Name starts with " + prefix);
	}

	@Configuration
	static class HazelcastServerAndClientConfig {

		@Bean
		public Config config() {
			return new Config();
		}

		@Bean
		public ClientConfig clientConfig() {
			return new ClientConfig();
		}

	}

}

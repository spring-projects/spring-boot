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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.test.context.ContextLoader;
import org.springframework.boot.test.context.StandardContextLoader;
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

	private final StandardContextLoader contextLoader = ContextLoader.standard()
			.autoConfig(HazelcastAutoConfiguration.class);

	@Test
	public void systemProperty() throws IOException {
		this.contextLoader
				.systemProperty(HazelcastClientConfiguration.CONFIG_SYSTEM_PROPERTY,
						"classpath:org/springframework/boot/autoconfigure/hazelcast/"
								+ "hazelcast-client-specific.xml")
				.load(context -> {
					HazelcastInstance hazelcastInstance = context
							.getBean(HazelcastInstance.class);
					assertThat(hazelcastInstance)
							.isInstanceOf(HazelcastClientProxy.class);
					assertThat(hazelcastInstance.getName()).startsWith("hz.client_");
				});
	}

	@Test
	public void explicitConfigFile() throws IOException {
		this.contextLoader
				.env("spring.hazelcast.config=org/springframework/boot/autoconfigure/"
						+ "hazelcast/hazelcast-client-specific.xml")
				.load(context -> {
					HazelcastInstance hazelcastInstance = context
							.getBean(HazelcastInstance.class);
					assertThat(hazelcastInstance)
							.isInstanceOf(HazelcastClientProxy.class);
					assertThat(hazelcastInstance.getName()).startsWith("hz.client_");
				});
	}

	@Test
	public void explicitConfigUrl() throws IOException {
		this.contextLoader.env("spring.hazelcast.config=hazelcast-client-default.xml")
				.load(context -> {
					HazelcastInstance hazelcastInstance = context
							.getBean(HazelcastInstance.class);
					assertThat(hazelcastInstance)
							.isInstanceOf(HazelcastClientProxy.class);
					assertThat(hazelcastInstance.getName()).startsWith("hz.client_");
				});
	}

	@Test
	public void unknownConfigFile() {
		this.contextLoader.env("spring.hazelcast.config=foo/bar/unknown.xml").loadAndFail(
				BeanCreationException.class,
				ex -> assertThat(ex.getMessage()).contains("foo/bar/unknown.xml"));
	}

	@Test
	public void clientConfigTakesPrecedence() {
		this.contextLoader.config(HazelcastServerAndClientConfig.class)
				.env("spring.hazelcast.config=this-is-ignored.xml").load(context -> {
					HazelcastInstance hazelcastInstance = context
							.getBean(HazelcastInstance.class);
					assertThat(hazelcastInstance)
							.isInstanceOf(HazelcastClientProxy.class);
				});
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

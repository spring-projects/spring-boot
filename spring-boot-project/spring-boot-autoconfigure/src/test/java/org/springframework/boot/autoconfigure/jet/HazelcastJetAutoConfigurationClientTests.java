/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.jet;

import com.hazelcast.config.Config;
import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.config.JetConfig;
import com.hazelcast.jet.impl.JetClientInstanceImpl;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HazelcastJetServerConfiguration} specific to the client.
 *
 * @author Ali Gurbuz
 */
@ClassPathExclusions({ "hazelcast-client*.jar" })
class HazelcastJetAutoConfigurationClientTests {

	/**
	 * Servers the test clients will connect to.
	 */
	private static JetInstance jetServer;

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(HazelcastJetAutoConfiguration.class));

	@BeforeAll
	static void init() {
		JetConfig jetConfig = new JetConfig();
		Config hazelcastConfig = jetConfig.getHazelcastConfig();
		hazelcastConfig.getGroupConfig().setName("boot").setPassword("test");
		hazelcastConfig.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
		jetServer = Jet.newJetInstance(jetConfig);
	}

	@AfterAll
	static void close() {
		if (jetServer != null) {
			jetServer.shutdown();
		}
	}

	@Test
	void systemPropertyWithXml() {
		this.contextRunner
				.withSystemProperties(HazelcastJetClientConfiguration.CONFIG_SYSTEM_PROPERTY
						+ "=classpath:org/springframework/boot/autoconfigure/jet/hazelcast-jet-client-specific.xml")
				.run(assertSpecificHazelcastJetClient("explicit-xml"));
	}

	@Test
	void systemPropertyWithYaml() {
		this.contextRunner
				.withSystemProperties(HazelcastJetClientConfiguration.CONFIG_SYSTEM_PROPERTY
						+ "=classpath:org/springframework/boot/autoconfigure/jet/hazelcast-jet-client-specific.yaml")
				.run(assertSpecificHazelcastJetClient("explicit-yaml"));
	}

	@Test
	void explicitConfigFileWithXml() {
		this.contextRunner
				.withPropertyValues("spring.hazelcast.jet.client.config=org/springframework/boot/autoconfigure/"
						+ "jet/hazelcast-jet-client-specific.xml")
				.run(assertSpecificHazelcastJetClient("explicit-xml"));
	}

	@Test
	void explicitConfigFileWithYaml() {
		this.contextRunner
				.withPropertyValues("spring.hazelcast.jet.client.config=org/springframework/boot/autoconfigure/"
						+ "jet/hazelcast-jet-client-specific.yaml")
				.run(assertSpecificHazelcastJetClient("explicit-yaml"));
	}

	@Test
	void explicitConfigUrlWithXml() {
		this.contextRunner
				.withPropertyValues("spring.hazelcast.jet.client.config=classpath:org/springframework/"
						+ "boot/autoconfigure/jet/hazelcast-jet-client-specific.xml")
				.run(assertSpecificHazelcastJetClient("explicit-xml"));
	}

	@Test
	void explicitConfigUrlWithYaml() {
		this.contextRunner
				.withPropertyValues("spring.hazelcast.jet.client.config=classpath:org/springframework/"
						+ "boot/autoconfigure/jet/hazelcast-jet-client-specific.yaml")
				.run(assertSpecificHazelcastJetClient("explicit-yaml"));
	}

	@Test
	void unknownConfigFile() {
		this.contextRunner.withPropertyValues("spring.hazelcast.jet.client.config=foo/bar/unknown.xml")
				.run((context) -> assertThat(context).getFailure().isInstanceOf(BeanCreationException.class)
						.hasMessageContaining("foo/bar/unknown.xml"));
	}

	private static ContextConsumer<AssertableApplicationContext> assertSpecificHazelcastJetClient(String label) {
		return (context) -> assertThat(context).getBean(JetInstance.class).isInstanceOf(JetInstance.class)
				.has(labelEqualTo(label));
	}

	private static Condition<JetInstance> labelEqualTo(String label) {
		return new Condition<>((o) -> ((JetClientInstanceImpl) o).getHazelcastClient().getClientConfig().getLabels()
				.stream().anyMatch((e) -> e.equals(label)), "Label equals to " + label);
	}

}

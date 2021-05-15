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

package org.springframework.boot.autoconfigure.hazelcast;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HazelcastAutoConfiguration} with full classpath.
 *
 * @author Stephane Nicoll
 */
class HazelcastAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(HazelcastAutoConfiguration.class));

	@Test
	void defaultConfigFile() {
		// no hazelcast-client.xml and hazelcast.xml is present in root classpath
		// this also asserts that XML has priority over YAML
		// as both hazelcast.yaml and hazelcast.xml in test classpath.
		this.contextRunner.run((context) -> {
			Config config = context.getBean(HazelcastInstance.class).getConfig();
			assertThat(config.getConfigurationUrl()).isEqualTo(new ClassPathResource("hazelcast.xml").getURL());
		});
	}

	@Test
	void hazelcastInstanceNotCreatedWhenJetIsPresent() {
		this.contextRunner.withClassLoader(new JetConfigClassLoader())
				.run((context) -> assertThat(context).doesNotHaveBean(HazelcastInstance.class));
	}

	/**
	 * A test {@link URLClassLoader} that emulates the default Hazelcast Jet configuration
	 * file exists on the classpath.
	 */
	static class JetConfigClassLoader extends URLClassLoader {

		private static final Resource FALLBACK = new ClassPathResource("hazelcast.yaml");

		JetConfigClassLoader() {
			super(new URL[0], JetConfigClassLoader.class.getClassLoader());
		}

		@Override
		public URL getResource(String name) {
			if (name.equals("hazelcast-jet-default.yaml")) {
				return getEmulatedJetConfigUrl();
			}
			return super.getResource(name);
		}

		private URL getEmulatedJetConfigUrl() {
			try {
				return FALLBACK.getURL();
			}
			catch (IOException ex) {
				throw new IllegalArgumentException(ex);
			}
		}

	}

}

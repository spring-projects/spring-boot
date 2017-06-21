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

import com.hazelcast.core.HazelcastInstance;
import org.junit.Test;

import org.springframework.boot.test.context.ContextLoader;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HazelcastAutoConfiguration} with full classpath.
 *
 * @author Stephane Nicoll
 */
public class HazelcastAutoConfigurationTests {

	private final ContextLoader contextLoader = new ContextLoader()
			.autoConfig(HazelcastAutoConfiguration.class);

	@Test
	public void defaultConfigFile() throws IOException {
		// no hazelcast-client.xml and hazelcast.xml is present in root classpath
		this.contextLoader.load(context -> {
			HazelcastInstance hazelcastInstance = context.getBean(HazelcastInstance.class);
			assertThat(hazelcastInstance.getConfig().getConfigurationUrl())
					.isEqualTo(new ClassPathResource("hazelcast.xml").getURL());
		});
	}

}

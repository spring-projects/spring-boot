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

import com.hazelcast.core.HazelcastInstance;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.ClassPathOverrides;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link HazelcastAutoConfiguration} when Hazelcast Jet is present. Spring Boot
 * should not auto-create Hazelcast IMDG instances when Hazelcast Jet is present.
 *
 * @author Ali Gurbuz
 */
@ClassPathOverrides("com.hazelcast.jet:hazelcast-jet:4.0")
class HazelcastAutoConfigurationJetTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(HazelcastAutoConfiguration.class));

	@Test
	void testHazelcastInstanceNotCreatedWhenJetIsPresent() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(HazelcastInstance.class));
	}

}

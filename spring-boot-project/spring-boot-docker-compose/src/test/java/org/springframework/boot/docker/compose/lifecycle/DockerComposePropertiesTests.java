/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.docker.compose.lifecycle;

import java.io.File;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DockerComposeProperties}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DockerComposePropertiesTests {

	@Test
	void getWhenNoPropertiesReturnsNew() {
		Binder binder = new Binder(new MapConfigurationPropertySource());
		DockerComposeProperties properties = DockerComposeProperties.get(binder);
		assertThat(properties.getFile()).isNull();
		assertThat(properties.getLifecycleManagement()).isEqualTo(LifecycleManagement.START_AND_STOP);
		assertThat(properties.getHost()).isNull();
		assertThat(properties.getStartup().getCommand()).isEqualTo(StartupCommand.UP);
		assertThat(properties.getShutdown().getCommand()).isEqualTo(ShutdownCommand.DOWN);
		assertThat(properties.getShutdown().getTimeout()).isEqualTo(Duration.ofSeconds(10));
		assertThat(properties.getProfiles().getActive()).isEmpty();
	}

	@Test
	void getWhenPropertiesReturnsBound() {
		Map<String, String> source = new LinkedHashMap<>();
		source.put("spring.docker.compose.file", "my-compose.yml");
		source.put("spring.docker.compose.lifecycle-management", "start-only");
		source.put("spring.docker.compose.host", "myhost");
		source.put("spring.docker.compose.startup.command", "start");
		source.put("spring.docker.compose.shutdown.command", "stop");
		source.put("spring.docker.compose.shutdown.timeout", "5s");
		source.put("spring.docker.compose.profiles.active", "myprofile");
		Binder binder = new Binder(new MapConfigurationPropertySource(source));
		DockerComposeProperties properties = DockerComposeProperties.get(binder);
		assertThat(properties.getFile()).isEqualTo(new File("my-compose.yml"));
		assertThat(properties.getLifecycleManagement()).isEqualTo(LifecycleManagement.START_ONLY);
		assertThat(properties.getHost()).isEqualTo("myhost");
		assertThat(properties.getStartup().getCommand()).isEqualTo(StartupCommand.START);
		assertThat(properties.getShutdown().getCommand()).isEqualTo(ShutdownCommand.STOP);
		assertThat(properties.getShutdown().getTimeout()).isEqualTo(Duration.ofSeconds(5));
		assertThat(properties.getProfiles().getActive()).containsExactly("myprofile");
	}

}

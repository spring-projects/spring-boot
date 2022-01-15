/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.test.context;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link SpringBootTest @SpringBootTest} with an
 * {@link ActiveProfiles @ActiveProfiles} annotation.
 *
 * @author Johnny Lim
 * @author Phillip Webb
 */
@SpringBootTest
@ActiveProfiles({ "test1", "test2" })
@ContextConfiguration(loader = SpringBootTestWithActiveProfilesAndEnvironmentPropertyTests.Loader.class)
class SpringBootTestWithActiveProfilesAndEnvironmentPropertyTests {

	@Autowired
	private Environment environment;

	@Test
	void getActiveProfiles() {
		assertThat(this.environment.getActiveProfiles()).containsOnly("test1", "test2");
	}

	@Configuration
	static class Config {

	}

	static class Loader extends SpringBootContextLoader {

		@Override
		protected ConfigurableEnvironment getEnvironment() {
			ConfigurableEnvironment environment = new StandardEnvironment();
			MutablePropertySources sources = environment.getPropertySources();
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("spring.profiles.active", "local");
			sources.addLast(new MapPropertySource("profiletest", map));
			return environment;
		}

	}

}

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

package org.springframework.boot.actuate.info;

import java.util.Collections;
import java.util.Map;

import org.junit.Test;

import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EnvironmentInfoContributor}.
 *
 * @author Stephane Nicoll
 */
public class EnvironmentInfoContributorTests {

	private final StandardEnvironment environment = new StandardEnvironment();

	@Test
	public void extractOnlyInfoProperty() {
		EnvironmentTestUtils.addEnvironment(this.environment, "info.app=my app",
				"info.version=1.0.0", "foo=bar");
		Info actual = contributeFrom(this.environment);
		assertThat(actual.get("app", String.class)).isEqualTo("my app");
		assertThat(actual.get("version", String.class)).isEqualTo("1.0.0");
		assertThat(actual.getDetails().size()).isEqualTo(2);
	}

	@Test
	public void extractNoEntry() {
		EnvironmentTestUtils.addEnvironment(this.environment, "foo=bar");
		Info actual = contributeFrom(this.environment);
		assertThat(actual.getDetails().size()).isEqualTo(0);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void propertiesFromEnvironmentShouldBindCorrectly() throws Exception {
		MutablePropertySources propertySources = this.environment.getPropertySources();
		propertySources.addFirst(new SystemEnvironmentPropertySource("system",
				Collections.singletonMap("INFO_ENVIRONMENT_FOO", "green")));
		Info actual = contributeFrom(this.environment);
		assertThat(actual.get("environment", Map.class)).containsEntry("foo", "green");
	}

	private static Info contributeFrom(ConfigurableEnvironment environment) {
		EnvironmentInfoContributor contributor = new EnvironmentInfoContributor(
				environment);
		Info.Builder builder = new Info.Builder();
		contributor.contribute(builder);
		return builder.build();
	}

}

/*
 * Copyright 2012-2016 the original author or authors.
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

import java.util.Properties;

import org.junit.Test;

import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

public class EnvironmentInfoProviderTests {

	@Test
	public void provide_HasTwoRelevantEntries_ShowOnlyRelevantEntries() throws Exception {
		String expectedAppName = "my app name";
		String expectedLanguage = "da-DK";

		Properties properties = new Properties();
		properties.setProperty("info.app", expectedAppName);
		properties.setProperty("info.lang", expectedLanguage);
		properties.setProperty("logging.path", "notExpected");

		PropertySource<?> propertySource = new PropertiesPropertySource("mysettings", properties);

		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addLast(propertySource);

		EnvironmentInfoProvider environmentInfoProvider = new EnvironmentInfoProvider(environment);

		Info actual = environmentInfoProvider.provide();
		assertThat(actual.getDetails().size()).isEqualTo(2);
		assertThat((String) actual.get("app")).isEqualTo(expectedAppName);
		assertThat((String) actual.get("lang")).isEqualTo(expectedLanguage);
	}

	@Test
	public void provide_HasNoRelevantEntries_NoEntries() throws Exception {
		Properties properties = new Properties();
		properties.setProperty("logging.path", "notExpected");

		PropertySource<?> propertySource = new PropertiesPropertySource("mysettings", properties);

		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addLast(propertySource);

		EnvironmentInfoProvider environmentInfoProvider = new EnvironmentInfoProvider(environment);

		Info actual = environmentInfoProvider.provide();
		assertThat(actual.getDetails().size()).isEqualTo(0);
	}


	@Test
	public void provide_HasNoEntries_NoEntries() throws Exception {
		EnvironmentInfoProvider environmentInfoProvider = new EnvironmentInfoProvider(new StandardEnvironment());

		Info actual = environmentInfoProvider.provide();
		assertThat(actual.getDetails().size()).isEqualTo(0);
	}

}

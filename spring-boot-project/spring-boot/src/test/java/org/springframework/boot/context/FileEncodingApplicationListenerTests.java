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

package org.springframework.boot.context;

import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.context.support.TestPropertySourceUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link FileEncodingApplicationListener}.
 *
 * @author Dave Syer
 */
class FileEncodingApplicationListenerTests {

	private final FileEncodingApplicationListener initializer = new FileEncodingApplicationListener();

	private final ConfigurableEnvironment environment = new StandardEnvironment();

	private final ApplicationEnvironmentPreparedEvent event = new ApplicationEnvironmentPreparedEvent(
			new SpringApplication(), new String[0], this.environment);

	@Test
	void testIllegalState() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.mandatory_file_encoding=FOO");
		ConfigurationPropertySources.attach(this.environment);
		assertThatIllegalStateException().isThrownBy(() -> this.initializer.onApplicationEvent(this.event));
	}

	@Test
	void testSunnyDayNothingMandated() {
		this.initializer.onApplicationEvent(this.event);
	}

	@Test
	void testSunnyDayMandated() {
		assertThat(System.getProperty("file.encoding")).isNotNull();
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.mandatory_file_encoding:" + System.getProperty("file.encoding"));
		ConfigurationPropertySources.attach(this.environment);
		this.initializer.onApplicationEvent(this.event);
	}

}

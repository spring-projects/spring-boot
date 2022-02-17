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

package org.springframework.boot.autoconfigure.jsonb;

import jakarta.json.bind.Jsonb;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JsonbAutoConfiguration} when there is no provider available.
 *
 * @author Andy Wilkinson
 */
@ClassPathExclusions("yasson-*.jar")
class JsonbAutoConfigurationWithNoProviderTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(JsonbAutoConfiguration.class));

	@Test
	void jsonbBacksOffWhenThereIsNoProvider() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(Jsonb.class));
	}

}

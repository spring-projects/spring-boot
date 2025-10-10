/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.webflux.test.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.boot.webflux.autoconfigure.error.ErrorWebFluxAutoConfiguration;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.autoconfigure.AutoConfigurationImportedCondition.importedAutoConfiguration;

/**
 * Tests for the auto-configuration imported by {@link WebFluxTest @WebFluxTest}.
 *
 * @author Stephane Nicoll
 * @author Artsiom Yudovin
 * @author Ali Dehghani
 * @author Madhura Bhave
 */
@WebFluxTest
class WebFluxTestAutoConfigurationIntegrationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void messageSourceAutoConfigurationIsImported() {
		assertThat(this.applicationContext).has(importedAutoConfiguration(MessageSourceAutoConfiguration.class));
	}

	@Test
	void validationAutoConfigurationIsImported() {
		assertThat(this.applicationContext).has(importedAutoConfiguration(ValidationAutoConfiguration.class));
	}

	@Test
	void errorWebFluxAutoConfigurationIsImported() {
		assertThat(this.applicationContext).has(importedAutoConfiguration(ErrorWebFluxAutoConfiguration.class));
	}

}

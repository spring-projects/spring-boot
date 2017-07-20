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

package org.springframework.boot.autoconfigure.security.oauth2.client;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OAuth2RestOperationsConfiguration}.
 *
 * @author Madhura Bhave
 */
public class OAuth2RestOperationsConfigurationTests {

	private ConfigurableApplicationContext context;

	private ConfigurableEnvironment environment = new StandardEnvironment();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void clientIdConditionMatches() throws Exception {
		TestPropertyValues.of("security.oauth2.client.client-id=acme")
				.applyTo(this.environment);
		this.context = new SpringApplicationBuilder(
				OAuth2RestOperationsConfiguration.class).environment(this.environment)
						.web(WebApplicationType.NONE).run();
		assertThat(this.context.getBean(OAuth2RestOperationsConfiguration.class))
				.isNotNull();
	}

	@Test
	public void clientIdConditionDoesNotMatch() throws Exception {
		this.context = new SpringApplicationBuilder(
				OAuth2RestOperationsConfiguration.class).environment(this.environment)
						.web(WebApplicationType.NONE).run();
		this.thrown.expect(NoSuchBeanDefinitionException.class);
		this.context.getBean(OAuth2RestOperationsConfiguration.class);
	}

}

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

package org.springframework.boot.actuate.autoconfigure.web.server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ManagementContextFactory}.
 *
 * author Yongjun Hong
 */
class ManagementContextFactoryTests {

	@Test
	void createManagementContextCopiesManagementPropertySources() {
		ApplicationContext parentContext = mock(ApplicationContext.class);
		ConfigurableEnvironment parentEnvironment = mock(ConfigurableEnvironment.class);
		MutablePropertySources parentPropertySources = new MutablePropertySources();
		PropertySource<?> managementPropertySource = new PropertySource<>("managementProperty") {
			@Override
			public Object getProperty(String name) {
				return null;
			}
		};
		parentPropertySources.addLast(managementPropertySource);
		given(parentEnvironment.getPropertySources()).willReturn(parentPropertySources);
		given(parentContext.getEnvironment()).willReturn(parentEnvironment);

		ManagementContextFactory factory = new ManagementContextFactory(WebApplicationType.SERVLET, null);

		ConfigurableApplicationContext managementContext = factory.createManagementContext(parentContext);

		ConfigurableEnvironment childEnvironment = managementContext.getEnvironment();
		assertThat(childEnvironment.getPropertySources().contains("managementProperty")).isTrue();
	}

}
/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.zero.actuate.autoconfigure;

import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.zero.actuate.properties.ManagementServerProperties;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link ManagementServerPropertiesAutoConfiguration}.
 * 
 * @author Phillip Webb
 */
public class ManagementServerPropertiesAutoConfigurationTests {

	@Test
	public void defaultManagementServerProperties() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				ManagementServerPropertiesAutoConfiguration.class);
		assertThat(context.getBean(ManagementServerProperties.class).getPort(),
				nullValue());
		context.close();
	}

	@Test
	public void definedManagementServerProperties() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				Config.class, ManagementServerPropertiesAutoConfiguration.class);
		assertThat(context.getBean(ManagementServerProperties.class).getPort(),
				equalTo(Integer.valueOf(123)));
		context.close();
	}

	@Configuration
	public static class Config {

		@Bean
		public ManagementServerProperties managementServerProperties() {
			ManagementServerProperties properties = new ManagementServerProperties();
			properties.setPort(123);
			return properties;
		}

	}

}

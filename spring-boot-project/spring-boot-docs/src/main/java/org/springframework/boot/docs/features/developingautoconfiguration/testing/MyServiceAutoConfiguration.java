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

package org.springframework.boot.docs.features.developingautoconfiguration.testing;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.docs.features.developingautoconfiguration.testing.MyServiceAutoConfiguration.UserProperties;
import org.springframework.context.annotation.Bean;

/**
 * MyServiceAutoConfiguration class.
 */
@AutoConfiguration
@ConditionalOnClass(MyService.class)
@EnableConfigurationProperties(UserProperties.class)
public class MyServiceAutoConfiguration {

	/**
	 * Creates a new instance of MyService if no other bean of type MyService is present
	 * in the application context.
	 * @param properties the UserProperties object containing the necessary configuration
	 * properties for MyService
	 * @return a new instance of MyService with the specified name
	 */
	@Bean
	@ConditionalOnMissingBean
	public MyService userService(UserProperties properties) {
		return new MyService(properties.getName());
	}

	/**
	 * UserProperties class.
	 */
	@ConfigurationProperties("user")
	public static class UserProperties {

		private String name = "test";

		/**
		 * Returns the name of the UserProperties object.
		 * @return the name of the UserProperties object
		 */
		public String getName() {
			return this.name;
		}

		/**
		 * Sets the name of the user.
		 * @param name the name to set
		 */
		public void setName(String name) {
			this.name = name;
		}

	}

}

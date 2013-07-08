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

package org.springframework.zero.sample.integration;

import org.springframework.bootstrap.context.properties.ConfigurationProperties;

@ConfigurationProperties(name = "service", ignoreUnknownFields = false)
public class ServiceProperties {

	private String greeting = "Hello";

	public String getGreeting() {
		return this.greeting;
	}

	public void setGreeting(String greeting) {
		this.greeting = greeting;
	}

}

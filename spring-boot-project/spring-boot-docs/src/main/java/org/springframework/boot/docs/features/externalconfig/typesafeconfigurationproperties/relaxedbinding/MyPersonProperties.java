/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.docs.features.externalconfig.typesafeconfigurationproperties.relaxedbinding;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MyPersonProperties class.
 */
@ConfigurationProperties(prefix = "my.main-project.person")
public class MyPersonProperties {

	private String firstName;

	/**
	 * Returns the first name of the person.
	 * @return the first name of the person
	 */
	public String getFirstName() {
		return this.firstName;
	}

	/**
	 * Sets the first name of the person.
	 * @param firstName the first name to be set
	 */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

}

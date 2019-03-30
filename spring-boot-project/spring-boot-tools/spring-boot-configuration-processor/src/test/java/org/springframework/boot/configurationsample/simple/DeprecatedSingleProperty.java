/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.configurationsample.simple;

import org.springframework.boot.configurationsample.ConfigurationProperties;
import org.springframework.boot.configurationsample.DeprecatedConfigurationProperty;

/**
 * Configuration properties with a single deprecated element.
 *
 * @author Phillip Webb
 */
@ConfigurationProperties("singledeprecated")
public class DeprecatedSingleProperty {

	private String newName;

	@Deprecated
	@DeprecatedConfigurationProperty(reason = "renamed", replacement = "singledeprecated.new-name")
	public String getName() {
		return getNewName();
	}

	@Deprecated
	public void setName(String name) {
		setNewName(name);
	}

	public String getNewName() {
		return this.newName;
	}

	public void setNewName(String newName) {
		this.newName = newName;
	}

}

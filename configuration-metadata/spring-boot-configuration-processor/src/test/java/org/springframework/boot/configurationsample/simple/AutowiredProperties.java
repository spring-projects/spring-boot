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

package org.springframework.boot.configurationsample.simple;

import org.springframework.boot.configurationsample.TestAutowired;

/**
 * Properties with autowired constructor.
 *
 * @author Madhura Bhave
 */
public class AutowiredProperties {

	/**
	 * The name of this simple properties.
	 */
	private String theName;

	@TestAutowired
	public AutowiredProperties(String theName) {
		this.theName = theName;
	}

	public String getTheName() {
		return this.theName;
	}

	public void setTheName(String name) {
		this.theName = name;
	}

}

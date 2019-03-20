/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.configurationsample.specific;

import org.springframework.boot.configurationsample.ConfigurationProperties;

/**
 * Demonstrates that invalid accessors are ignored.
 *
 * @author Stephane Nicoll
 */
@ConfigurationProperties(prefix = "config")
public class InvalidAccessorProperties {

	private String name;

	private boolean flag;

	public void set(String name) {
		this.name = name;
	}

	public String get() {
		return this.name;
	}

	public void setFlag(boolean flag) {
		this.flag = flag;
	}

	public boolean is() {
		return this.flag;
	}

}

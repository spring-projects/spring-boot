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

package org.springframework.boot.configurationsample.immutable;

import org.springframework.boot.configurationsample.ConfigurationProperties;
import org.springframework.boot.configurationsample.DefaultValue;

/**
 * @author Madhura Bhave
 */
@ConfigurationProperties("immutable")
public class ImmutableDeducedConstructorBindingProperties {

	/**
	 * The name of these properties.
	 */
	private final String theName;

	/**
	 * A simple flag.
	 */
	private final boolean flag;

	public ImmutableDeducedConstructorBindingProperties(@DefaultValue("boot") String theName, boolean flag) {
		this.theName = theName;
		this.flag = flag;
	}

	public String getTheName() {
		return this.theName;
	}

	public boolean isFlag() {
		return this.flag;
	}

}

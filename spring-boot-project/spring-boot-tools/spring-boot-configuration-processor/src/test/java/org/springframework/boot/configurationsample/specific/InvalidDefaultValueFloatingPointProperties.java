/*
 * Copyright 2012-2019 the original author or authors.
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
import org.springframework.boot.configurationsample.DefaultValue;

/**
 * Demonstrates that an invalid default floating point value leads to a compilation
 * failure.
 *
 * @author Stephane Nicoll
 */
@ConfigurationProperties("test")
public class InvalidDefaultValueFloatingPointProperties {

	private final Double ratio;

	public InvalidDefaultValueFloatingPointProperties(@DefaultValue("55.55.33") Double ratio) {
		this.ratio = ratio;
	}

	public Double getRatio() {
		return this.ratio;
	}

}

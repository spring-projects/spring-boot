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

package org.springframework.boot.context.properties;

import org.jspecify.annotations.Nullable;

/**
 * A {@link ConfigurationProperties @ConfigurationProperties} with an additional
 * single-arg public constructor. Used in {@link ConfigurationPropertiesTests}.
 *
 * @author Madhura Bhave
 */
@ConfigurationProperties("test")
public class WithPublicStringConstructorProperties {

	private @Nullable String a;

	public WithPublicStringConstructorProperties() {
	}

	public WithPublicStringConstructorProperties(String a) {
		this.a = a;
	}

	public @Nullable String getA() {
		return this.a;
	}

	public void setA(@Nullable String a) {
		this.a = a;
	}

}

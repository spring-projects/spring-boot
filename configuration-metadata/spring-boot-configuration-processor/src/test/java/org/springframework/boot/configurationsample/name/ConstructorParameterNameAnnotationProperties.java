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

package org.springframework.boot.configurationsample.name;

import org.springframework.boot.configurationsample.ConfigurationProperties;
import org.springframework.boot.configurationsample.DefaultValue;
import org.springframework.boot.configurationsample.Name;

/**
 * Constructor properties making use of {@code @Name}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
@ConfigurationProperties("named")
public class ConstructorParameterNameAnnotationProperties {

	/**
	 * Imports to apply.
	 */
	private final String imports;

	/**
	 * Whether default mode is enabled.
	 */
	private final boolean defaultValue;

	public ConstructorParameterNameAnnotationProperties(@Name("import") String imports,
			@Name("default") @DefaultValue("true") boolean defaultValue) {
		this.imports = imports;
		this.defaultValue = defaultValue;
	}

	public String getImports() {
		return this.imports;
	}

	public boolean isDefaultValue() {
		return this.defaultValue;
	}

}

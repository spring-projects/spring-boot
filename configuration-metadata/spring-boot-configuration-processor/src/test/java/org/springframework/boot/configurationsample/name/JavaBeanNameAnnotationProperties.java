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

import org.springframework.boot.configurationsample.TestConfigurationProperties;
import org.springframework.boot.configurationsample.TestName;

/**
 * Java bean properties making use of {@code @Name}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
@TestConfigurationProperties("named")
public class JavaBeanNameAnnotationProperties {

	/**
	 * Imports to apply.
	 */
	@TestName("import")
	private String imports;

	/**
	 * Whether default mode is enabled.
	 */
	@TestName("default")
	private boolean defaultValue = true;

	public String getImports() {
		return this.imports;
	}

	public void setImports(String imports) {
		this.imports = imports;
	}

	public boolean isDefaultValue() {
		return this.defaultValue;
	}

	public void setDefaultValue(boolean defaultValue) {
		this.defaultValue = defaultValue;
	}

}

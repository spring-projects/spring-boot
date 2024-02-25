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

package org.springframework.boot.logging.logback;

import ch.qos.logback.core.model.NamedModel;

/**
 * Logback {@link NamedModel model} to support {@code <springProperty>} tags. Allows
 * Logback properties to be sourced from the Spring environment.
 *
 * @author Andy Wilkinson
 * @see SpringPropertyAction
 * @see SpringPropertyModelHandler
 */
class SpringPropertyModel extends NamedModel {

	private String scope;

	private String defaultValue;

	private String source;

	/**
	 * Returns the scope of the SpringPropertyModel.
	 * @return the scope of the SpringPropertyModel
	 */
	String getScope() {
		return this.scope;
	}

	/**
	 * Sets the scope of the SpringPropertyModel.
	 * @param scope the scope to be set for the SpringPropertyModel
	 */
	void setScope(String scope) {
		this.scope = scope;
	}

	/**
	 * Returns the default value of the SpringPropertyModel.
	 * @return the default value of the SpringPropertyModel
	 */
	String getDefaultValue() {
		return this.defaultValue;
	}

	/**
	 * Sets the default value for the SpringPropertyModel.
	 * @param defaultValue the default value to be set
	 */
	void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	/**
	 * Returns the source of the SpringPropertyModel.
	 * @return the source of the SpringPropertyModel
	 */
	String getSource() {
		return this.source;
	}

	/**
	 * Sets the source of the SpringPropertyModel.
	 * @param source the source to set
	 */
	void setSource(String source) {
		this.source = source;
	}

}

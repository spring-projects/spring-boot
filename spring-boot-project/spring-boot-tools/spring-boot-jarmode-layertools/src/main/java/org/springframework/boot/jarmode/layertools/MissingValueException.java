/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.jarmode.layertools;

/**
 * Exception thrown when a required value is not provided for an option.
 *
 * @author Scott Frederick
 */
class MissingValueException extends RuntimeException {

	private final String optionName;

	/**
     * Constructs a new MissingValueException with the specified option name.
     * 
     * @param optionName the name of the missing option
     */
    MissingValueException(String optionName) {
		this.optionName = optionName;
	}

	/**
     * Returns the message of the MissingValueException.
     * 
     * @return the message of the MissingValueException
     */
    @Override
	public String getMessage() {
		return "--" + this.optionName;
	}

}

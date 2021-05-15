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

package org.springframework.boot.context.config;

import org.springframework.boot.origin.OriginProvider;

/**
 * {@link ConfigDataNotFoundException} thrown when a {@link ConfigData} cannot be found.
 *
 * @author Phillip Webb
 * @since 2.4.0
 */
public abstract class ConfigDataNotFoundException extends ConfigDataException implements OriginProvider {

	/**
	 * Create a new {@link ConfigDataNotFoundException} instance.
	 * @param message the exception message
	 * @param cause the exception cause
	 */
	ConfigDataNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Return a description of actual referenced item that could not be found.
	 * @return a description of the referenced items
	 */
	public abstract String getReferenceDescription();

}

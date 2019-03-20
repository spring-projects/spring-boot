/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.configurationprocessor.metadata;

import javax.tools.Diagnostic;

/**
 * Thrown to indicate that some meta-data is invalid. Define the severity to determine
 * whether it has to fail the build.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
@SuppressWarnings("serial")
public class InvalidConfigurationMetadataException extends RuntimeException {

	private final Diagnostic.Kind kind;

	public InvalidConfigurationMetadataException(String message, Diagnostic.Kind kind) {
		super(message);
		this.kind = kind;
	}

	public Diagnostic.Kind getKind() {
		return this.kind;
	}

}

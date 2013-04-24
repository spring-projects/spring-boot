/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.bootstrap.cli;

/**
 * Exception thrown when an unknown root option is specified. This only applies to
 * {@link Command#isOptionCommand() option command}.
 * 
 * @author Phillip Webb
 */
class NoSuchOptionException extends BootstrapCliException {

	private static final long serialVersionUID = 1L;

	public NoSuchOptionException(String name) {
		super("Unknown option: --" + name, Option.SHOW_USAGE);
	}
}

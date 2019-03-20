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

package org.springframework.boot.autoconfigure.web;

import org.springframework.beans.factory.annotation.Value;

/**
 * Configuration properties for web error handling.
 *
 * @author Michael Stummvoll
 * @author Stephane Nicoll
 * @since 1.3.0
 */
public class ErrorProperties {

	/**
	 * Path of the error controller.
	 */
	@Value("${error.path:/error}")
	private String path = "/error";

	/**
	 * When to include a "stacktrace" attribute.
	 */
	private IncludeStacktrace includeStacktrace = IncludeStacktrace.NEVER;

	public String getPath() {
		return this.path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public IncludeStacktrace getIncludeStacktrace() {
		return this.includeStacktrace;
	}

	public void setIncludeStacktrace(IncludeStacktrace includeStacktrace) {
		this.includeStacktrace = includeStacktrace;
	}

	/**
	 * Include Stacktrace attribute options.
	 */
	public enum IncludeStacktrace {

		/**
		 * Never add stacktrace information.
		 */
		NEVER,

		/**
		 * Always add stacktrace information.
		 */
		ALWAYS,

		/**
		 * Add stacktrace information when the "trace" request parameter is "true".
		 */
		ON_TRACE_PARAM

	}

}

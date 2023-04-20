/*
 * Copyright 2012-2021 the original author or authors.
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
 * @author Vedran Pavic
 * @author Scott Frederick
 * @since 1.3.0
 */
public class ErrorProperties {

	/**
	 * Path of the error controller.
	 */
	@Value("${error.path:/error}")
	private String path = "/error";

	/**
	 * Include the "exception" attribute.
	 */
	private boolean includeException;

	/**
	 * When to include the "trace" attribute.
	 */
	private IncludeAttribute includeStacktrace = IncludeAttribute.NEVER;

	/**
	 * When to include "message" attribute.
	 */
	private IncludeAttribute includeMessage = IncludeAttribute.NEVER;

	/**
	 * When to include "errors" attribute.
	 */
	private IncludeAttribute includeBindingErrors = IncludeAttribute.NEVER;

	private final Whitelabel whitelabel = new Whitelabel();

	public String getPath() {
		return this.path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public boolean isIncludeException() {
		return this.includeException;
	}

	public void setIncludeException(boolean includeException) {
		this.includeException = includeException;
	}

	public IncludeAttribute getIncludeStacktrace() {
		return this.includeStacktrace;
	}

	public void setIncludeStacktrace(IncludeAttribute includeStacktrace) {
		this.includeStacktrace = includeStacktrace;
	}

	public IncludeAttribute getIncludeMessage() {
		return this.includeMessage;
	}

	public void setIncludeMessage(IncludeAttribute includeMessage) {
		this.includeMessage = includeMessage;
	}

	public IncludeAttribute getIncludeBindingErrors() {
		return this.includeBindingErrors;
	}

	public void setIncludeBindingErrors(IncludeAttribute includeBindingErrors) {
		this.includeBindingErrors = includeBindingErrors;
	}

	public Whitelabel getWhitelabel() {
		return this.whitelabel;
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
		 * Add stacktrace attribute when the appropriate request parameter is not "false".
		 */
		ON_PARAM

	}

	/**
	 * Include error attributes options.
	 */
	public enum IncludeAttribute {

		/**
		 * Never add error attribute.
		 */
		NEVER,

		/**
		 * Always add error attribute.
		 */
		ALWAYS,

		/**
		 * Add error attribute when the appropriate request parameter is not "false".
		 */
		ON_PARAM

	}

	public static class Whitelabel {

		/**
		 * Whether to enable the default error page displayed in browsers in case of a
		 * server error.
		 */
		private boolean enabled = true;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

	}

}

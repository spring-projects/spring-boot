/*
 * Copyright 2012-2024 the original author or authors.
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

	/**
	 * When to include "path" attribute.
	 */
	private IncludeAttribute includePath = IncludeAttribute.ALWAYS;

	private final Whitelabel whitelabel = new Whitelabel();

	/**
	 * Returns the path of the ErrorProperties object.
	 * @return the path of the ErrorProperties object
	 */
	public String getPath() {
		return this.path;
	}

	/**
	 * Sets the path for the ErrorProperties object.
	 * @param path the path to be set
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * Returns a boolean value indicating whether the exception should be included.
	 * @return true if the exception should be included, false otherwise
	 */
	public boolean isIncludeException() {
		return this.includeException;
	}

	/**
	 * Sets whether to include the exception in the error properties.
	 * @param includeException true to include the exception, false otherwise
	 */
	public void setIncludeException(boolean includeException) {
		this.includeException = includeException;
	}

	/**
	 * Returns the IncludeAttribute object representing whether the stacktrace should be
	 * included in the error response.
	 * @return the IncludeAttribute object representing whether the stacktrace should be
	 * included in the error response
	 */
	public IncludeAttribute getIncludeStacktrace() {
		return this.includeStacktrace;
	}

	/**
	 * Sets the includeStacktrace attribute.
	 * @param includeStacktrace the includeStacktrace attribute to be set
	 */
	public void setIncludeStacktrace(IncludeAttribute includeStacktrace) {
		this.includeStacktrace = includeStacktrace;
	}

	/**
	 * Returns the IncludeAttribute object representing the include message.
	 * @return the IncludeAttribute object representing the include message
	 */
	public IncludeAttribute getIncludeMessage() {
		return this.includeMessage;
	}

	/**
	 * Sets the includeMessage attribute.
	 * @param includeMessage the includeMessage attribute to be set
	 */
	public void setIncludeMessage(IncludeAttribute includeMessage) {
		this.includeMessage = includeMessage;
	}

	/**
	 * Returns the includeBindingErrors attribute.
	 * @return the includeBindingErrors attribute
	 */
	public IncludeAttribute getIncludeBindingErrors() {
		return this.includeBindingErrors;
	}

	/**
	 * Sets the includeBindingErrors attribute.
	 * @param includeBindingErrors the includeBindingErrors to set
	 */
	public void setIncludeBindingErrors(IncludeAttribute includeBindingErrors) {
		this.includeBindingErrors = includeBindingErrors;
	}

	/**
	 * Returns the include path attribute.
	 * @return the include path attribute
	 */
	public IncludeAttribute getIncludePath() {
		return this.includePath;
	}

	/**
	 * Sets the include path for the ErrorProperties class.
	 * @param includePath the include path to be set
	 */
	public void setIncludePath(IncludeAttribute includePath) {
		this.includePath = includePath;
	}

	/**
	 * Returns the Whitelabel object associated with this ErrorProperties instance.
	 * @return the Whitelabel object
	 */
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

	/**
	 * Whitelabel class.
	 */
	public static class Whitelabel {

		/**
		 * Whether to enable the default error page displayed in browsers in case of a
		 * server error.
		 */
		private boolean enabled = true;

		/**
		 * Returns the current status of the enabled flag.
		 * @return true if the enabled flag is set, false otherwise.
		 */
		public boolean isEnabled() {
			return this.enabled;
		}

		/**
		 * Sets the enabled status of the Whitelabel.
		 * @param enabled the enabled status to be set
		 */
		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

	}

}

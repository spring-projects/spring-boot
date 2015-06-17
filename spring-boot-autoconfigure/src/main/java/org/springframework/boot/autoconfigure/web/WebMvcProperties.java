/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.web;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.DefaultMessageCodesResolver;

/**
 * {@link ConfigurationProperties properties} for Spring MVC.
 *
 * @author Phillip Webb
 * @author Sébastien Deleuze
 * @since 1.1
 */
@ConfigurationProperties("spring.mvc")
public class WebMvcProperties {

	/**
	 * Formatting strategy for message codes (PREFIX_ERROR_CODE, POSTFIX_ERROR_CODE).
	 */
	private DefaultMessageCodesResolver.Format messageCodesResolverFormat;

	/**
	 * Locale to use.
	 */
	private String locale;

	/**
	 * Date format to use (e.g. dd/MM/yyyy).
	 */
	private String dateFormat;

	/**
	 * If the the content of the "default" model should be ignored during redirect
	 * scenarios.
	 */
	private boolean ignoreDefaultModelOnRedirect = true;

	private final Async async = new Async();

	public DefaultMessageCodesResolver.Format getMessageCodesResolverFormat() {
		return this.messageCodesResolverFormat;
	}

	public void setMessageCodesResolverFormat(
			DefaultMessageCodesResolver.Format messageCodesResolverFormat) {
		this.messageCodesResolverFormat = messageCodesResolverFormat;
	}

	public String getLocale() {
		return this.locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	public String getDateFormat() {
		return this.dateFormat;
	}

	public void setDateFormat(String dateFormat) {
		this.dateFormat = dateFormat;
	}

	public boolean isIgnoreDefaultModelOnRedirect() {
		return this.ignoreDefaultModelOnRedirect;
	}

	public void setIgnoreDefaultModelOnRedirect(boolean ignoreDefaultModelOnRedirect) {
		this.ignoreDefaultModelOnRedirect = ignoreDefaultModelOnRedirect;
	}

	public Async getAsync() {
		return this.async;
	}

	public static class Async {

		/**
		 * Amount of time (in milliseconds) before asynchronous request handling times
		 * out. If this value is not set, the default timeout of the underlying
		 * implementation is used, e.g. 10 seconds on Tomcat with Servlet 3.
		 */
		private Long requestTimeout;

		public Long getRequestTimeout() {
			return this.requestTimeout;
		}

		public void setRequestTimeout(Long requestTimeout) {
			this.requestTimeout = requestTimeout;
		}

	}
}

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

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.validation.DefaultMessageCodesResolver;

/**
 * {@link ConfigurationProperties properties} for Spring MVC.
 *
 * @author Phillip Webb
 * @author Sébastien Deleuze
 * @author Stephane Nicoll
 * @author Eddú Meléndez
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
	private Locale locale;

	/**
	 * Date format to use (e.g. dd/MM/yyyy).
	 */
	private String dateFormat;

	/**
	 * Dispatch TRACE requests to the FrameworkServlet doService method.
	 */
	private boolean dispatchTraceRequest = false;

	/**
	 * Dispatch OPTIONS requests to the FrameworkServlet doService method.
	 */
	private boolean dispatchOptionsRequest = false;

	/**
	 * If the content of the "default" model should be ignored during redirect scenarios.
	 */
	private boolean ignoreDefaultModelOnRedirect = true;

	/**
	 * If a "NoHandlerFoundException" should be thrown if no Handler was found to process
	 * a request.
	 */
	private boolean throwExceptionIfNoHandlerFound = false;

	/**
	 * Maps file extensions to media types for content negotiation, e.g. yml->text/yaml.
	 */
	private Map<String, MediaType> mediaTypes = new LinkedHashMap<String, MediaType>();

	/**
	 * Path pattern used for static resources.
	 */
	private String staticPathPattern = "/**";

	private final Async async = new Async();

	private final View view = new View();

	public DefaultMessageCodesResolver.Format getMessageCodesResolverFormat() {
		return this.messageCodesResolverFormat;
	}

	public void setMessageCodesResolverFormat(
			DefaultMessageCodesResolver.Format messageCodesResolverFormat) {
		this.messageCodesResolverFormat = messageCodesResolverFormat;
	}

	public Locale getLocale() {
		return this.locale;
	}

	public void setLocale(Locale locale) {
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

	public boolean isThrowExceptionIfNoHandlerFound() {
		return this.throwExceptionIfNoHandlerFound;
	}

	public void setThrowExceptionIfNoHandlerFound(
			boolean throwExceptionIfNoHandlerFound) {
		this.throwExceptionIfNoHandlerFound = throwExceptionIfNoHandlerFound;
	}

	public Map<String, MediaType> getMediaTypes() {
		return this.mediaTypes;
	}

	public void setMediaTypes(Map<String, MediaType> mediaTypes) {
		this.mediaTypes = mediaTypes;
	}

	public boolean isDispatchOptionsRequest() {
		return this.dispatchOptionsRequest;
	}

	public void setDispatchOptionsRequest(boolean dispatchOptionsRequest) {
		this.dispatchOptionsRequest = dispatchOptionsRequest;
	}

	public boolean isDispatchTraceRequest() {
		return this.dispatchTraceRequest;
	}

	public void setDispatchTraceRequest(boolean dispatchTraceRequest) {
		this.dispatchTraceRequest = dispatchTraceRequest;
	}

	public String getStaticPathPattern() {
		return this.staticPathPattern;
	}

	public void setStaticPathPattern(String staticPathPattern) {
		this.staticPathPattern = staticPathPattern;
	}

	public Async getAsync() {
		return this.async;
	}

	public View getView() {
		return this.view;
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

	public static class View {

		/**
		 * Spring MVC view prefix.
		 */
		@Value("${spring.view.prefix:}")
		private String prefix;

		/**
		 * Spring MVC view suffix.
		 */
		@Value("${spring.view.suffix:}")
		private String suffix;

		public String getPrefix() {
			return this.prefix;
		}

		public void setPrefix(String prefix) {
			this.prefix = prefix;
		}

		public String getSuffix() {
			return this.suffix;
		}

		public void setSuffix(String suffix) {
			this.suffix = suffix;
		}

	}

}

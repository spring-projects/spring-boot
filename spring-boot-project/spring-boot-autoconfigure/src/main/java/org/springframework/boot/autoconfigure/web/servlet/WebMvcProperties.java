/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.servlet;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.validation.DefaultMessageCodesResolver;

/**
 * {@link ConfigurationProperties Properties} for Spring MVC.
 *
 * @author Phillip Webb
 * @author Sébastien Deleuze
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @author Brian Clozel
 * @author Vedran Pavic
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "spring.mvc")
public class WebMvcProperties {

	/**
	 * Formatting strategy for message codes. For instance, 'PREFIX_ERROR_CODE'.
	 */
	private DefaultMessageCodesResolver.Format messageCodesResolverFormat;

	private final Format format = new Format();

	/**
	 * Whether to dispatch TRACE requests to the FrameworkServlet doService method.
	 */
	private boolean dispatchTraceRequest = false;

	/**
	 * Whether to dispatch OPTIONS requests to the FrameworkServlet doService method.
	 */
	private boolean dispatchOptionsRequest = true;

	/**
	 * Whether to publish a ServletRequestHandledEvent at the end of each request.
	 */
	private boolean publishRequestHandledEvents = true;

	/**
	 * Whether a "NoHandlerFoundException" should be thrown if no Handler was found to
	 * process a request.
	 * @deprecated since 3.2.0 for removal in 3.4.0
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	private boolean throwExceptionIfNoHandlerFound = true;

	/**
	 * Whether logging of (potentially sensitive) request details at DEBUG and TRACE level
	 * is allowed.
	 */
	private boolean logRequestDetails;

	/**
	 * Whether to enable warn logging of exceptions resolved by a
	 * "HandlerExceptionResolver", except for "DefaultHandlerExceptionResolver".
	 */
	private boolean logResolvedException = false;

	/**
	 * Path pattern used for static resources.
	 */
	private String staticPathPattern = "/**";

	/**
	 * Path pattern used for WebJar assets.
	 */
	private String webjarsPathPattern = "/webjars/**";

	private final Async async = new Async();

	private final Servlet servlet = new Servlet();

	private final View view = new View();

	private final Contentnegotiation contentnegotiation = new Contentnegotiation();

	private final Pathmatch pathmatch = new Pathmatch();

	private final Problemdetails problemdetails = new Problemdetails();

	/**
	 * Returns the format of the message codes resolver.
	 * @return the format of the message codes resolver
	 */
	public DefaultMessageCodesResolver.Format getMessageCodesResolverFormat() {
		return this.messageCodesResolverFormat;
	}

	/**
	 * Sets the format for the message codes resolver.
	 * @param messageCodesResolverFormat the format to set for the message codes resolver
	 */
	public void setMessageCodesResolverFormat(DefaultMessageCodesResolver.Format messageCodesResolverFormat) {
		this.messageCodesResolverFormat = messageCodesResolverFormat;
	}

	/**
	 * Returns the format of the WebMvcProperties.
	 * @return the format of the WebMvcProperties
	 */
	public Format getFormat() {
		return this.format;
	}

	/**
	 * Returns a boolean value indicating whether the publish request handled events flag
	 * is enabled or not.
	 * @return true if the publish request handled events flag is enabled, false otherwise
	 */
	public boolean isPublishRequestHandledEvents() {
		return this.publishRequestHandledEvents;
	}

	/**
	 * Sets the flag indicating whether to publish request handled events.
	 * @param publishRequestHandledEvents the flag indicating whether to publish request
	 * handled events
	 */
	public void setPublishRequestHandledEvents(boolean publishRequestHandledEvents) {
		this.publishRequestHandledEvents = publishRequestHandledEvents;
	}

	/**
	 * Returns the value of the throwExceptionIfNoHandlerFound property.
	 * @return the value of the throwExceptionIfNoHandlerFound property
	 * @deprecated This method is deprecated since version 3.2.0 and is scheduled for
	 * removal. The DispatcherServlet property is deprecated for removal and should no
	 * longer need to be configured. Please use an alternative approach to handle the case
	 * when no handler is found. This method will be removed in a future release. Consider
	 * using the setThrowExceptionIfNoHandlerFound(boolean) method instead.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	@DeprecatedConfigurationProperty(
			reason = "DispatcherServlet property is deprecated for removal and should no longer need to be configured",
			since = "3.2.0")
	public boolean isThrowExceptionIfNoHandlerFound() {
		return this.throwExceptionIfNoHandlerFound;
	}

	/**
	 * Sets the flag indicating whether an exception should be thrown if no handler is
	 * found.
	 * @param throwExceptionIfNoHandlerFound the flag indicating whether an exception
	 * should be thrown if no handler is found
	 * @deprecated This method is deprecated since version 3.2.0 and will be removed in a
	 * future release. Please use an alternative method instead.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setThrowExceptionIfNoHandlerFound(boolean throwExceptionIfNoHandlerFound) {
		this.throwExceptionIfNoHandlerFound = throwExceptionIfNoHandlerFound;
	}

	/**
	 * Returns a boolean value indicating whether to log request details.
	 * @return true if request details should be logged, false otherwise
	 */
	public boolean isLogRequestDetails() {
		return this.logRequestDetails;
	}

	/**
	 * Sets the flag to determine whether to log request details.
	 * @param logRequestDetails the flag indicating whether to log request details
	 */
	public void setLogRequestDetails(boolean logRequestDetails) {
		this.logRequestDetails = logRequestDetails;
	}

	/**
	 * Returns a boolean value indicating whether the resolved exceptions should be
	 * logged.
	 * @return {@code true} if the resolved exceptions should be logged, {@code false}
	 * otherwise.
	 */
	public boolean isLogResolvedException() {
		return this.logResolvedException;
	}

	/**
	 * Sets the flag indicating whether to log resolved exceptions.
	 * @param logResolvedException true to enable logging of resolved exceptions, false
	 * otherwise
	 */
	public void setLogResolvedException(boolean logResolvedException) {
		this.logResolvedException = logResolvedException;
	}

	/**
	 * Returns a boolean value indicating whether the request is a dispatch options
	 * request.
	 * @return {@code true} if the request is a dispatch options request, {@code false}
	 * otherwise.
	 */
	public boolean isDispatchOptionsRequest() {
		return this.dispatchOptionsRequest;
	}

	/**
	 * Sets the flag indicating whether to dispatch an OPTIONS request to the handler
	 * method.
	 * @param dispatchOptionsRequest the flag indicating whether to dispatch an OPTIONS
	 * request
	 */
	public void setDispatchOptionsRequest(boolean dispatchOptionsRequest) {
		this.dispatchOptionsRequest = dispatchOptionsRequest;
	}

	/**
	 * Returns a boolean value indicating whether a dispatch trace request is enabled.
	 * @return {@code true} if dispatch trace request is enabled, {@code false} otherwise.
	 */
	public boolean isDispatchTraceRequest() {
		return this.dispatchTraceRequest;
	}

	/**
	 * Sets the flag indicating whether to dispatch trace requests.
	 * @param dispatchTraceRequest true to dispatch trace requests, false otherwise
	 */
	public void setDispatchTraceRequest(boolean dispatchTraceRequest) {
		this.dispatchTraceRequest = dispatchTraceRequest;
	}

	/**
	 * Returns the static path pattern.
	 * @return the static path pattern
	 */
	public String getStaticPathPattern() {
		return this.staticPathPattern;
	}

	/**
	 * Sets the static path pattern for serving static resources.
	 * @param staticPathPattern the static path pattern to be set
	 */
	public void setStaticPathPattern(String staticPathPattern) {
		this.staticPathPattern = staticPathPattern;
	}

	/**
	 * Returns the webjars path pattern.
	 * @return the webjars path pattern
	 */
	public String getWebjarsPathPattern() {
		return this.webjarsPathPattern;
	}

	/**
	 * Sets the path pattern for Webjars.
	 * @param webjarsPathPattern the path pattern for Webjars
	 */
	public void setWebjarsPathPattern(String webjarsPathPattern) {
		this.webjarsPathPattern = webjarsPathPattern;
	}

	/**
	 * Returns the Async object associated with this WebMvcProperties instance.
	 * @return the Async object
	 */
	public Async getAsync() {
		return this.async;
	}

	/**
	 * Returns the servlet associated with this instance.
	 * @return the servlet associated with this instance
	 */
	public Servlet getServlet() {
		return this.servlet;
	}

	/**
	 * Returns the view associated with this WebMvcProperties object.
	 * @return the view associated with this WebMvcProperties object
	 */
	public View getView() {
		return this.view;
	}

	/**
	 * Returns the content negotiation configuration for this WebMvcProperties instance.
	 * @return the content negotiation configuration
	 */
	public Contentnegotiation getContentnegotiation() {
		return this.contentnegotiation;
	}

	/**
	 * Returns the Pathmatch object associated with this WebMvcProperties instance.
	 * @return the Pathmatch object
	 */
	public Pathmatch getPathmatch() {
		return this.pathmatch;
	}

	/**
	 * Returns the Problemdetails object associated with this WebMvcProperties instance.
	 * @return the Problemdetails object
	 */
	public Problemdetails getProblemdetails() {
		return this.problemdetails;
	}

	/**
	 * Async class.
	 */
	public static class Async {

		/**
		 * Amount of time before asynchronous request handling times out. If this value is
		 * not set, the default timeout of the underlying implementation is used.
		 */
		private Duration requestTimeout;

		/**
		 * Returns the request timeout duration.
		 * @return the request timeout duration
		 */
		public Duration getRequestTimeout() {
			return this.requestTimeout;
		}

		/**
		 * Sets the request timeout for the asynchronous operation.
		 * @param requestTimeout the duration of the request timeout
		 */
		public void setRequestTimeout(Duration requestTimeout) {
			this.requestTimeout = requestTimeout;
		}

	}

	/**
	 * Servlet class.
	 */
	public static class Servlet {

		/**
		 * Path of the dispatcher servlet. Setting a custom value for this property is not
		 * compatible with the PathPatternParser matching strategy.
		 */
		private String path = "/";

		/**
		 * Load on startup priority of the dispatcher servlet.
		 */
		private int loadOnStartup = -1;

		/**
		 * Returns the path of the servlet.
		 * @return the path of the servlet
		 */
		public String getPath() {
			return this.path;
		}

		/**
		 * Sets the path for the servlet.
		 * @param path the path to be set
		 * @throws IllegalArgumentException if the path is null or contains wildcards
		 */
		public void setPath(String path) {
			Assert.notNull(path, "Path must not be null");
			Assert.isTrue(!path.contains("*"), "Path must not contain wildcards");
			this.path = path;
		}

		/**
		 * Returns the value of the loadOnStartup property.
		 * @return the value of the loadOnStartup property
		 */
		public int getLoadOnStartup() {
			return this.loadOnStartup;
		}

		/**
		 * Sets the value for the load on startup parameter.
		 * @param loadOnStartup the value to set for the load on startup parameter
		 */
		public void setLoadOnStartup(int loadOnStartup) {
			this.loadOnStartup = loadOnStartup;
		}

		/**
		 * Returns the servlet mapping for the current servlet.
		 * @return the servlet mapping
		 */
		public String getServletMapping() {
			if (this.path.equals("") || this.path.equals("/")) {
				return "/";
			}
			if (this.path.endsWith("/")) {
				return this.path + "*";
			}
			return this.path + "/*";
		}

		/**
		 * Returns the full path for a given path by appending the servlet prefix.
		 * @param path the path to be processed
		 * @return the full path with the servlet prefix
		 */
		public String getPath(String path) {
			String prefix = getServletPrefix();
			if (!path.startsWith("/")) {
				path = "/" + path;
			}
			return prefix + path;
		}

		/**
		 * Returns the servlet prefix.
		 * @return the servlet prefix
		 */
		public String getServletPrefix() {
			String result = this.path;
			int index = result.indexOf('*');
			if (index != -1) {
				result = result.substring(0, index);
			}
			if (result.endsWith("/")) {
				result = result.substring(0, result.length() - 1);
			}
			return result;
		}

	}

	/**
	 * View class.
	 */
	public static class View {

		/**
		 * Spring MVC view prefix.
		 */
		private String prefix;

		/**
		 * Spring MVC view suffix.
		 */
		private String suffix;

		/**
		 * Returns the prefix used in the View.
		 * @return the prefix used in the View
		 */
		public String getPrefix() {
			return this.prefix;
		}

		/**
		 * Sets the prefix for the View.
		 * @param prefix the prefix to be set
		 */
		public void setPrefix(String prefix) {
			this.prefix = prefix;
		}

		/**
		 * Returns the suffix of the View.
		 * @return the suffix of the View
		 */
		public String getSuffix() {
			return this.suffix;
		}

		/**
		 * Sets the suffix for the view.
		 * @param suffix the suffix to be set for the view
		 */
		public void setSuffix(String suffix) {
			this.suffix = suffix;
		}

	}

	/**
	 * Contentnegotiation class.
	 */
	public static class Contentnegotiation {

		/**
		 * Whether a request parameter ("format" by default) should be used to determine
		 * the requested media type.
		 */
		private boolean favorParameter = false;

		/**
		 * Map file extensions to media types for content negotiation. For instance, yml
		 * to text/yaml.
		 */
		private Map<String, MediaType> mediaTypes = new LinkedHashMap<>();

		/**
		 * Query parameter name to use when "favor-parameter" is enabled.
		 */
		private String parameterName;

		/**
		 * Returns whether the favor parameter is enabled or not.
		 * @return true if the favor parameter is enabled, false otherwise
		 */
		public boolean isFavorParameter() {
			return this.favorParameter;
		}

		/**
		 * Sets the favorParameter flag for content negotiation.
		 * @param favorParameter the flag indicating whether to favor parameter for
		 * content negotiation
		 */
		public void setFavorParameter(boolean favorParameter) {
			this.favorParameter = favorParameter;
		}

		/**
		 * Returns the map of media types.
		 * @return the map of media types
		 */
		public Map<String, MediaType> getMediaTypes() {
			return this.mediaTypes;
		}

		/**
		 * Sets the media types for content negotiation.
		 * @param mediaTypes a map containing the media types to be set
		 */
		public void setMediaTypes(Map<String, MediaType> mediaTypes) {
			this.mediaTypes = mediaTypes;
		}

		/**
		 * Returns the name of the parameter.
		 * @return the name of the parameter
		 */
		public String getParameterName() {
			return this.parameterName;
		}

		/**
		 * Sets the parameter name for content negotiation.
		 * @param parameterName the name of the parameter to be set
		 */
		public void setParameterName(String parameterName) {
			this.parameterName = parameterName;
		}

	}

	/**
	 * Pathmatch class.
	 */
	public static class Pathmatch {

		/**
		 * Choice of strategy for matching request paths against registered mappings.
		 */
		private MatchingStrategy matchingStrategy = MatchingStrategy.PATH_PATTERN_PARSER;

		/**
		 * Returns the matching strategy used by the Pathmatch class.
		 * @return the matching strategy
		 */
		public MatchingStrategy getMatchingStrategy() {
			return this.matchingStrategy;
		}

		/**
		 * Sets the matching strategy for the Pathmatch class.
		 * @param matchingStrategy the matching strategy to be set
		 */
		public void setMatchingStrategy(MatchingStrategy matchingStrategy) {
			this.matchingStrategy = matchingStrategy;
		}

	}

	/**
	 * Format class.
	 */
	public static class Format {

		/**
		 * Date format to use, for example 'dd/MM/yyyy'.
		 */
		private String date;

		/**
		 * Time format to use, for example 'HH:mm:ss'.
		 */
		private String time;

		/**
		 * Date-time format to use, for example 'yyyy-MM-dd HH:mm:ss'.
		 */
		private String dateTime;

		/**
		 * Returns the date.
		 * @return the date
		 */
		public String getDate() {
			return this.date;
		}

		/**
		 * Sets the date for the Format class.
		 * @param date the date to be set
		 */
		public void setDate(String date) {
			this.date = date;
		}

		/**
		 * Returns the current time.
		 * @return the current time as a String
		 */
		public String getTime() {
			return this.time;
		}

		/**
		 * Sets the time value for the Format object.
		 * @param time the time value to be set
		 */
		public void setTime(String time) {
			this.time = time;
		}

		/**
		 * Returns the date and time in the specified format.
		 * @return the date and time in the specified format
		 */
		public String getDateTime() {
			return this.dateTime;
		}

		/**
		 * Sets the date and time for the Format object.
		 * @param dateTime the date and time to be set
		 */
		public void setDateTime(String dateTime) {
			this.dateTime = dateTime;
		}

	}

	/**
	 * Matching strategy options.
	 *
	 * @since 2.4.0
	 */
	public enum MatchingStrategy {

		/**
		 * Use the {@code AntPathMatcher} implementation.
		 */
		ANT_PATH_MATCHER,

		/**
		 * Use the {@code PathPatternParser} implementation.
		 */
		PATH_PATTERN_PARSER

	}

	/**
	 * Problemdetails class.
	 */
	public static class Problemdetails {

		/**
		 * Whether RFC 7807 Problem Details support should be enabled.
		 */
		private boolean enabled = false;

		/**
		 * Returns the current status of the enabled flag.
		 * @return {@code true} if the flag is enabled, {@code false} otherwise.
		 */
		public boolean isEnabled() {
			return this.enabled;
		}

		/**
		 * Sets the enabled status of the Problemdetails object.
		 * @param enabled the boolean value indicating whether the Problemdetails object
		 * is enabled or not
		 */
		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

	}

}

/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.webmvc.autoconfigure;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Name;
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
 * @since 4.0.0
 */
@ConfigurationProperties("spring.mvc")
public class WebMvcProperties {

	/**
	 * Formatting strategy for message codes. For instance, 'PREFIX_ERROR_CODE'.
	 */
	private DefaultMessageCodesResolver.@Nullable Format messageCodesResolverFormat;

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

	private final Apiversion apiversion = new Apiversion();

	public DefaultMessageCodesResolver.@Nullable Format getMessageCodesResolverFormat() {
		return this.messageCodesResolverFormat;
	}

	public void setMessageCodesResolverFormat(DefaultMessageCodesResolver.@Nullable Format messageCodesResolverFormat) {
		this.messageCodesResolverFormat = messageCodesResolverFormat;
	}

	public Format getFormat() {
		return this.format;
	}

	public boolean isPublishRequestHandledEvents() {
		return this.publishRequestHandledEvents;
	}

	public void setPublishRequestHandledEvents(boolean publishRequestHandledEvents) {
		this.publishRequestHandledEvents = publishRequestHandledEvents;
	}

	public boolean isLogRequestDetails() {
		return this.logRequestDetails;
	}

	public void setLogRequestDetails(boolean logRequestDetails) {
		this.logRequestDetails = logRequestDetails;
	}

	public boolean isLogResolvedException() {
		return this.logResolvedException;
	}

	public void setLogResolvedException(boolean logResolvedException) {
		this.logResolvedException = logResolvedException;
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

	public String getWebjarsPathPattern() {
		return this.webjarsPathPattern;
	}

	public void setWebjarsPathPattern(String webjarsPathPattern) {
		this.webjarsPathPattern = webjarsPathPattern;
	}

	public Async getAsync() {
		return this.async;
	}

	public Servlet getServlet() {
		return this.servlet;
	}

	public View getView() {
		return this.view;
	}

	public Contentnegotiation getContentnegotiation() {
		return this.contentnegotiation;
	}

	public Pathmatch getPathmatch() {
		return this.pathmatch;
	}

	public Problemdetails getProblemdetails() {
		return this.problemdetails;
	}

	public Apiversion getApiversion() {
		return this.apiversion;
	}

	public static class Async {

		/**
		 * Amount of time before asynchronous request handling times out. If this value is
		 * not set, the default timeout of the underlying implementation is used.
		 */
		private @Nullable Duration requestTimeout;

		public @Nullable Duration getRequestTimeout() {
			return this.requestTimeout;
		}

		public void setRequestTimeout(@Nullable Duration requestTimeout) {
			this.requestTimeout = requestTimeout;
		}

	}

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

		public String getPath() {
			return this.path;
		}

		public void setPath(String path) {
			Assert.notNull(path, "'path' must not be null");
			Assert.isTrue(!path.contains("*"), "'path' must not contain wildcards");
			this.path = path;
		}

		public int getLoadOnStartup() {
			return this.loadOnStartup;
		}

		public void setLoadOnStartup(int loadOnStartup) {
			this.loadOnStartup = loadOnStartup;
		}

		public String getServletMapping() {
			if (this.path.isEmpty() || this.path.equals("/")) {
				return "/";
			}
			if (this.path.endsWith("/")) {
				return this.path + "*";
			}
			return this.path + "/*";
		}

		public String getPath(String path) {
			String prefix = getServletPrefix();
			if (!path.startsWith("/")) {
				path = "/" + path;
			}
			return prefix + path;
		}

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

	public static class View {

		/**
		 * Spring MVC view prefix.
		 */
		private @Nullable String prefix;

		/**
		 * Spring MVC view suffix.
		 */
		private @Nullable String suffix;

		public @Nullable String getPrefix() {
			return this.prefix;
		}

		public void setPrefix(@Nullable String prefix) {
			this.prefix = prefix;
		}

		public @Nullable String getSuffix() {
			return this.suffix;
		}

		public void setSuffix(@Nullable String suffix) {
			this.suffix = suffix;
		}

	}

	public static class Contentnegotiation {

		/**
		 * Whether a request parameter ("format" by default) should be used to determine
		 * the requested media type.
		 */
		private boolean favorParameter = false;

		/**
		 * Query parameter name to use when "favor-parameter" is enabled.
		 */
		private @Nullable String parameterName;

		/**
		 * Map file extensions to media types for content negotiation. For instance, yml
		 * to text/yaml.
		 */
		private Map<String, MediaType> mediaTypes = new LinkedHashMap<>();

		/**
		 * List of default content types to be used when no specific content type is
		 * requested.
		 */
		private List<MediaType> defaultContentTypes = new ArrayList<>();

		public boolean isFavorParameter() {
			return this.favorParameter;
		}

		public void setFavorParameter(boolean favorParameter) {
			this.favorParameter = favorParameter;
		}

		public @Nullable String getParameterName() {
			return this.parameterName;
		}

		public void setParameterName(@Nullable String parameterName) {
			this.parameterName = parameterName;
		}

		public Map<String, MediaType> getMediaTypes() {
			return this.mediaTypes;
		}

		public void setMediaTypes(Map<String, MediaType> mediaTypes) {
			this.mediaTypes = mediaTypes;
		}

		public List<MediaType> getDefaultContentTypes() {
			return this.defaultContentTypes;
		}

		public void setDefaultContentTypes(List<MediaType> defaultContentTypes) {
			this.defaultContentTypes = defaultContentTypes;
		}

	}

	public static class Pathmatch {

		/**
		 * Choice of strategy for matching request paths against registered mappings.
		 */
		private MatchingStrategy matchingStrategy = MatchingStrategy.PATH_PATTERN_PARSER;

		public MatchingStrategy getMatchingStrategy() {
			return this.matchingStrategy;
		}

		public void setMatchingStrategy(MatchingStrategy matchingStrategy) {
			this.matchingStrategy = matchingStrategy;
		}

	}

	public static class Format {

		/**
		 * Date format to use, for example 'dd/MM/yyyy'. Used for formatting of
		 * java.util.Date and java.time.LocalDate.
		 */
		private @Nullable String date;

		/**
		 * Time format to use, for example 'HH:mm:ss'. Used for formatting of java.time's
		 * LocalTime and OffsetTime.
		 */
		private @Nullable String time;

		/**
		 * Date-time format to use, for example 'yyyy-MM-dd HH:mm:ss'. Used for formatting
		 * of java.time's LocalDateTime, OffsetDateTime, and ZonedDateTime.
		 */
		private @Nullable String dateTime;

		public @Nullable String getDate() {
			return this.date;
		}

		public void setDate(@Nullable String date) {
			this.date = date;
		}

		public @Nullable String getTime() {
			return this.time;
		}

		public void setTime(@Nullable String time) {
			this.time = time;
		}

		public @Nullable String getDateTime() {
			return this.dateTime;
		}

		public void setDateTime(@Nullable String dateTime) {
			this.dateTime = dateTime;
		}

	}

	/**
	 * Matching strategy options.
	 */
	public enum MatchingStrategy {

		/**
		 * Use the {@code AntPathMatcher} implementation.
		 * @deprecated since 4.0.0 for removal in 4.2.0 in favor of
		 * {@link #PATH_PATTERN_PARSER}
		 */
		@Deprecated(since = "4.0.0", forRemoval = true)
		ANT_PATH_MATCHER,

		/**
		 * Use the {@code PathPatternParser} implementation.
		 */
		PATH_PATTERN_PARSER

	}

	/**
	 * Problem Details.
	 */
	public static class Problemdetails {

		/**
		 * Whether RFC 9457 Problem Details support should be enabled.
		 */
		private boolean enabled = false;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

	}

	/**
	 * API Version.
	 */
	public static class Apiversion {

		/**
		 * Whether the API version is required with each request.
		 */
		private @Nullable Boolean required;

		/**
		 * Default version that should be used for each request.
		 */
		@Name("default")
		private @Nullable String defaultVersion;

		/**
		 * Supported versions.
		 */
		private @Nullable List<String> supported;

		/**
		 * Whether supported versions should be detected from controllers.
		 */
		private @Nullable Boolean detectSupported;

		/**
		 * How version details should be inserted into requests.
		 */
		private final Use use = new Use();

		public @Nullable Boolean getRequired() {
			return this.required;
		}

		public void setRequired(@Nullable Boolean required) {
			this.required = required;
		}

		public @Nullable String getDefaultVersion() {
			return this.defaultVersion;
		}

		public void setDefaultVersion(@Nullable String defaultVersion) {
			this.defaultVersion = defaultVersion;
		}

		public @Nullable List<String> getSupported() {
			return this.supported;
		}

		public void setSupported(@Nullable List<String> supported) {
			this.supported = supported;
		}

		public @Nullable Boolean getDetectSupported() {
			return this.detectSupported;
		}

		public void setDetectSupported(@Nullable Boolean detectSupported) {
			this.detectSupported = detectSupported;
		}

		public Use getUse() {
			return this.use;
		}

		public static class Use {

			/**
			 * Use the HTTP header with the given name to obtain the version.
			 */
			private @Nullable String header;

			/**
			 * Use the query parameter with the given name to obtain the version.
			 */
			private @Nullable String queryParameter;

			/**
			 * Use the path segment at the given index to obtain the version.
			 */
			private @Nullable Integer pathSegment;

			/**
			 * Use the media type parameter with the given name to obtain the version.
			 */
			private Map<MediaType, String> mediaTypeParameter = new LinkedHashMap<>();

			public @Nullable String getHeader() {
				return this.header;
			}

			public void setHeader(@Nullable String header) {
				this.header = header;
			}

			public @Nullable String getQueryParameter() {
				return this.queryParameter;
			}

			public void setQueryParameter(@Nullable String queryParameter) {
				this.queryParameter = queryParameter;
			}

			public @Nullable Integer getPathSegment() {
				return this.pathSegment;
			}

			public void setPathSegment(@Nullable Integer pathSegment) {
				this.pathSegment = pathSegment;
			}

			public Map<MediaType, String> getMediaTypeParameter() {
				return this.mediaTypeParameter;
			}

			public void setMediaTypeParameter(Map<MediaType, String> mediaTypeParameter) {
				this.mediaTypeParameter = mediaTypeParameter;
			}

		}

	}

}

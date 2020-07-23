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

package org.springframework.boot.autoconfigure.web.servlet;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;
import org.springframework.boot.context.properties.IncompatibleConfigurationException;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.validation.DefaultMessageCodesResolver;

/**
 * {@link ConfigurationProperties properties} for Spring MVC.
 *
 * @author Phillip Webb
 * @author Sébastien Deleuze
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @author Brian Clozel
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "spring.mvc")
public class WebMvcProperties {

	/**
	 * Formatting strategy for message codes. For instance, `PREFIX_ERROR_CODE`.
	 */
	private DefaultMessageCodesResolver.Format messageCodesResolverFormat;

	/**
	 * Locale to use. By default, this locale is overridden by the "Accept-Language"
	 * header.
	 */
	private Locale locale;

	/**
	 * Define how the locale should be resolved.
	 */
	private LocaleResolver localeResolver = LocaleResolver.ACCEPT_HEADER;

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
	 * Whether the content of the "default" model should be ignored during redirect
	 * scenarios.
	 */
	private boolean ignoreDefaultModelOnRedirect = true;

	/**
	 * Whether to publish a ServletRequestHandledEvent at the end of each request.
	 */
	private boolean publishRequestHandledEvents = true;

	/**
	 * Whether a "NoHandlerFoundException" should be thrown if no Handler was found to
	 * process a request.
	 */
	private boolean throwExceptionIfNoHandlerFound = false;

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

	private final Async async = new Async();

	private final Servlet servlet = new Servlet();

	private final View view = new View();

	private final Contentnegotiation contentnegotiation = new Contentnegotiation();

	private final Pathmatch pathmatch = new Pathmatch();

	public DefaultMessageCodesResolver.Format getMessageCodesResolverFormat() {
		return this.messageCodesResolverFormat;
	}

	public void setMessageCodesResolverFormat(DefaultMessageCodesResolver.Format messageCodesResolverFormat) {
		this.messageCodesResolverFormat = messageCodesResolverFormat;
	}

	public Locale getLocale() {
		return this.locale;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	public LocaleResolver getLocaleResolver() {
		return this.localeResolver;
	}

	public void setLocaleResolver(LocaleResolver localeResolver) {
		this.localeResolver = localeResolver;
	}

	@Deprecated
	@DeprecatedConfigurationProperty(replacement = "spring.mvc.format.date")
	public String getDateFormat() {
		return this.format.getDate();
	}

	@Deprecated
	public void setDateFormat(String dateFormat) {
		this.format.setDate(dateFormat);
	}

	public Format getFormat() {
		return this.format;
	}

	public boolean isIgnoreDefaultModelOnRedirect() {
		return this.ignoreDefaultModelOnRedirect;
	}

	public void setIgnoreDefaultModelOnRedirect(boolean ignoreDefaultModelOnRedirect) {
		this.ignoreDefaultModelOnRedirect = ignoreDefaultModelOnRedirect;
	}

	public boolean isPublishRequestHandledEvents() {
		return this.publishRequestHandledEvents;
	}

	public void setPublishRequestHandledEvents(boolean publishRequestHandledEvents) {
		this.publishRequestHandledEvents = publishRequestHandledEvents;
	}

	public boolean isThrowExceptionIfNoHandlerFound() {
		return this.throwExceptionIfNoHandlerFound;
	}

	public void setThrowExceptionIfNoHandlerFound(boolean throwExceptionIfNoHandlerFound) {
		this.throwExceptionIfNoHandlerFound = throwExceptionIfNoHandlerFound;
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

	@SuppressWarnings("deprecation")
	public void checkConfiguration() {
		if (this.getPathmatch().getMatchingStrategy() == MatchingStrategy.PATH_PATTERN_PARSER) {
			if (this.getPathmatch().isUseSuffixPattern()) {
				throw new IncompatibleConfigurationException("spring.mvc.pathmatch.matching-strategy",
						"spring.mvc.pathmatch.use-suffix-pattern");
			}
			if (this.getPathmatch().isUseRegisteredSuffixPattern()) {
				throw new IncompatibleConfigurationException("spring.mvc.pathmatch.matching-strategy",
						"spring.mvc.pathmatch.use-registered-suffix-pattern");
			}
			if (!this.getServlet().getServletMapping().equals("/")) {
				throw new IncompatibleConfigurationException("spring.mvc.pathmatch.matching-strategy",
						"spring.mvc.servlet.path");
			}
		}
	}

	public static class Async {

		/**
		 * Amount of time before asynchronous request handling times out. If this value is
		 * not set, the default timeout of the underlying implementation is used.
		 */
		private Duration requestTimeout;

		public Duration getRequestTimeout() {
			return this.requestTimeout;
		}

		public void setRequestTimeout(Duration requestTimeout) {
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
			Assert.notNull(path, "Path must not be null");
			Assert.isTrue(!path.contains("*"), "Path must not contain wildcards");
			this.path = path;
		}

		public int getLoadOnStartup() {
			return this.loadOnStartup;
		}

		public void setLoadOnStartup(int loadOnStartup) {
			this.loadOnStartup = loadOnStartup;
		}

		public String getServletMapping() {
			if (this.path.equals("") || this.path.equals("/")) {
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
		private String prefix;

		/**
		 * Spring MVC view suffix.
		 */
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

	public static class Contentnegotiation {

		/**
		 * Whether the path extension in the URL path should be used to determine the
		 * requested media type. If enabled a request "/users.pdf" will be interpreted as
		 * a request for "application/pdf" regardless of the 'Accept' header.
		 */
		private boolean favorPathExtension = false;

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

		@DeprecatedConfigurationProperty(
				reason = "Use of path extensions for request mapping and for content negotiation is discouraged.")
		@Deprecated
		public boolean isFavorPathExtension() {
			return this.favorPathExtension;
		}

		@Deprecated
		public void setFavorPathExtension(boolean favorPathExtension) {
			this.favorPathExtension = favorPathExtension;
		}

		public boolean isFavorParameter() {
			return this.favorParameter;
		}

		public void setFavorParameter(boolean favorParameter) {
			this.favorParameter = favorParameter;
		}

		public Map<String, MediaType> getMediaTypes() {
			return this.mediaTypes;
		}

		public void setMediaTypes(Map<String, MediaType> mediaTypes) {
			this.mediaTypes = mediaTypes;
		}

		public String getParameterName() {
			return this.parameterName;
		}

		public void setParameterName(String parameterName) {
			this.parameterName = parameterName;
		}

	}

	public static class Pathmatch {

		/**
		 * Choice of strategy for matching request paths against registered mappings.
		 */
		private MatchingStrategy matchingStrategy = MatchingStrategy.ANT_PATH_MATCHER;

		/**
		 * Whether to use suffix pattern match (".*") when matching patterns to requests.
		 * If enabled a method mapped to "/users" also matches to "/users.*". Enabling
		 * this option is not compatible with the PathPatternParser matching strategy.
		 */
		private boolean useSuffixPattern = false;

		/**
		 * Whether suffix pattern matching should work only against extensions registered
		 * with "spring.mvc.contentnegotiation.media-types.*". This is generally
		 * recommended to reduce ambiguity and to avoid issues such as when a "." appears
		 * in the path for other reasons. Enabling this option is not compatible with the
		 * PathPatternParser matching strategy.
		 */
		private boolean useRegisteredSuffixPattern = false;

		public MatchingStrategy getMatchingStrategy() {
			return this.matchingStrategy;
		}

		public void setMatchingStrategy(MatchingStrategy matchingStrategy) {
			this.matchingStrategy = matchingStrategy;
		}

		@DeprecatedConfigurationProperty(
				reason = "Use of path extensions for request mapping and for content negotiation is discouraged.")
		@Deprecated
		public boolean isUseSuffixPattern() {
			return this.useSuffixPattern;
		}

		@Deprecated
		public void setUseSuffixPattern(boolean useSuffixPattern) {
			this.useSuffixPattern = useSuffixPattern;
		}

		@DeprecatedConfigurationProperty(
				reason = "Use of path extensions for request mapping and for content negotiation is discouraged.")
		@Deprecated
		public boolean isUseRegisteredSuffixPattern() {
			return this.useRegisteredSuffixPattern;
		}

		@Deprecated
		public void setUseRegisteredSuffixPattern(boolean useRegisteredSuffixPattern) {
			this.useRegisteredSuffixPattern = useRegisteredSuffixPattern;
		}

	}

	public static class Format {

		/**
		 * Date format to use, for example `dd/MM/yyyy`.
		 */
		private String date;

		/**
		 * Time format to use, for example `HH:mm:ss`.
		 */
		private String time;

		/**
		 * Date-time format to use, for example `yyyy-MM-dd HH:mm:ss`.
		 */
		private String dateTime;

		public String getDate() {
			return this.date;
		}

		public void setDate(String date) {
			this.date = date;
		}

		public String getTime() {
			return this.time;
		}

		public void setTime(String time) {
			this.time = time;
		}

		public String getDateTime() {
			return this.dateTime;
		}

		public void setDateTime(String dateTime) {
			this.dateTime = dateTime;
		}

	}

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

	public enum LocaleResolver {

		/**
		 * Always use the configured locale.
		 */
		FIXED,

		/**
		 * Use the "Accept-Language" header or the configured locale if the header is not
		 * set.
		 */
		ACCEPT_HEADER

	}

}

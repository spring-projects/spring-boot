/*
 * Copyright 2012-2019 the original author or authors.
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
import org.springframework.http.MediaType;
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

	/**
	 * Date format to use. For instance, `dd/MM/yyyy`.
	 */
	private String dateFormat;

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
	 * Whether a "NoHandlerFoundException" should be thrown if no Handler was found to
	 * process a request.
	 */
	private boolean throwExceptionIfNoHandlerFound = false;

	/**
	 * Whether to enable warn logging of exceptions resolved by a
	 * "HandlerExceptionResolver".
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

	public void setThrowExceptionIfNoHandlerFound(boolean throwExceptionIfNoHandlerFound) {
		this.throwExceptionIfNoHandlerFound = throwExceptionIfNoHandlerFound;
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

	public static class Async {

		/**
		 * Amount of time before asynchronous request handling times out. If this value is
		 * not set, the default timeout of the underlying implementation is used, e.g. 10
		 * seconds on Tomcat with Servlet 3.
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
		 * Load on startup priority of the dispatcher servlet.
		 */
		private int loadOnStartup = -1;

		public int getLoadOnStartup() {
			return this.loadOnStartup;
		}

		public void setLoadOnStartup(int loadOnStartup) {
			this.loadOnStartup = loadOnStartup;
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

		public boolean isFavorPathExtension() {
			return this.favorPathExtension;
		}

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
		 * Whether to use suffix pattern match (".*") when matching patterns to requests.
		 * If enabled a method mapped to "/users" also matches to "/users.*".
		 */
		private boolean useSuffixPattern = false;

		/**
		 * Whether suffix pattern matching should work only against extensions registered
		 * with "spring.mvc.contentnegotiation.media-types.*". This is generally
		 * recommended to reduce ambiguity and to avoid issues such as when a "." appears
		 * in the path for other reasons.
		 */
		private boolean useRegisteredSuffixPattern = false;

		public boolean isUseSuffixPattern() {
			return this.useSuffixPattern;
		}

		public void setUseSuffixPattern(boolean useSuffixPattern) {
			this.useSuffixPattern = useSuffixPattern;
		}

		public boolean isUseRegisteredSuffixPattern() {
			return this.useRegisteredSuffixPattern;
		}

		public void setUseRegisteredSuffixPattern(boolean useRegisteredSuffixPattern) {
			this.useRegisteredSuffixPattern = useRegisteredSuffixPattern;
		}

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

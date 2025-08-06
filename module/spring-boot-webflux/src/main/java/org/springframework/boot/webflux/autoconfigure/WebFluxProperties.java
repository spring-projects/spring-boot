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

package org.springframework.boot.webflux.autoconfigure;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Name;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

/**
 * {@link ConfigurationProperties Properties} for Spring WebFlux.
 *
 * @author Brian Clozel
 * @author Vedran Pavic
 * @since 4.0.0
 */
@ConfigurationProperties("spring.webflux")
public class WebFluxProperties {

	/**
	 * Base path for all web handlers.
	 */
	private @Nullable String basePath;

	private final Format format = new Format();

	private final Problemdetails problemdetails = new Problemdetails();

	private final Apiversion apiversion = new Apiversion();

	/**
	 * Path pattern used for static resources.
	 */
	private String staticPathPattern = "/**";

	/**
	 * Path pattern used for WebJar assets.
	 */
	private String webjarsPathPattern = "/webjars/**";

	public @Nullable String getBasePath() {
		return this.basePath;
	}

	public void setBasePath(@Nullable String basePath) {
		this.basePath = cleanBasePath(basePath);
	}

	private @Nullable String cleanBasePath(@Nullable String basePath) {
		String candidate = null;
		if (StringUtils.hasLength(basePath)) {
			candidate = basePath.strip();
		}
		if (StringUtils.hasText(candidate)) {
			if (!candidate.startsWith("/")) {
				candidate = "/" + candidate;
			}
			if (candidate.endsWith("/")) {
				candidate = candidate.substring(0, candidate.length() - 1);
			}
		}
		return candidate;
	}

	public Format getFormat() {
		return this.format;
	}

	public Problemdetails getProblemdetails() {
		return this.problemdetails;
	}

	public Apiversion getApiversion() {
		return this.apiversion;
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

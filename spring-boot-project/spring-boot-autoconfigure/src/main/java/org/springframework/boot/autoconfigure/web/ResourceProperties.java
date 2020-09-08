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

package org.springframework.boot.autoconfigure.web;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.http.CacheControl;

/**
 * Properties used to configure resource handling.
 *
 * @author Phillip Webb
 * @author Brian Clozel
 * @author Dave Syer
 * @author Venil Noronha
 * @author Kristine Jetzke
 * @since 1.1.0
 */
@ConfigurationProperties(prefix = "spring.resources", ignoreUnknownFields = false)
public class ResourceProperties {

	private static final String[] CLASSPATH_RESOURCE_LOCATIONS = { "classpath:/META-INF/resources/",
			"classpath:/resources/", "classpath:/static/", "classpath:/public/" };

	/**
	 * Locations of static resources. Defaults to classpath:[/META-INF/resources/,
	 * /resources/, /static/, /public/].
	 */
	private String[] staticLocations = CLASSPATH_RESOURCE_LOCATIONS;

	/**
	 * Whether to enable default resource handling.
	 */
	private boolean addMappings = true;

	private final Chain chain = new Chain();

	private final Cache cache = new Cache();

	public String[] getStaticLocations() {
		return this.staticLocations;
	}

	public void setStaticLocations(String[] staticLocations) {
		this.staticLocations = appendSlashIfNecessary(staticLocations);
	}

	private String[] appendSlashIfNecessary(String[] staticLocations) {
		String[] normalized = new String[staticLocations.length];
		for (int i = 0; i < staticLocations.length; i++) {
			String location = staticLocations[i];
			normalized[i] = location.endsWith("/") ? location : location + "/";
		}
		return normalized;
	}

	public boolean isAddMappings() {
		return this.addMappings;
	}

	public void setAddMappings(boolean addMappings) {
		this.addMappings = addMappings;
	}

	public Chain getChain() {
		return this.chain;
	}

	public Cache getCache() {
		return this.cache;
	}

	/**
	 * Configuration for the Spring Resource Handling chain.
	 */
	public static class Chain {

		/**
		 * Whether to enable the Spring Resource Handling chain. By default, disabled
		 * unless at least one strategy has been enabled.
		 */
		private Boolean enabled;

		/**
		 * Whether to enable caching in the Resource chain.
		 */
		private boolean cache = true;

		/**
		 * Whether to enable HTML5 application cache manifest rewriting.
		 */
		private boolean htmlApplicationCache = false;

		/**
		 * Whether to enable resolution of already compressed resources (gzip, brotli).
		 * Checks for a resource name with the '.gz' or '.br' file extensions.
		 */
		private boolean compressed = false;

		private final Strategy strategy = new Strategy();

		/**
		 * Return whether the resource chain is enabled. Return {@code null} if no
		 * specific settings are present.
		 * @return whether the resource chain is enabled or {@code null} if no specified
		 * settings are present.
		 */
		public Boolean getEnabled() {
			return getEnabled(getStrategy().getFixed().isEnabled(), getStrategy().getContent().isEnabled(),
					this.enabled);
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public boolean isCache() {
			return this.cache;
		}

		public void setCache(boolean cache) {
			this.cache = cache;
		}

		public Strategy getStrategy() {
			return this.strategy;
		}

		@DeprecatedConfigurationProperty(reason = "The appcache manifest feature is being removed from browsers.")
		@Deprecated
		public boolean isHtmlApplicationCache() {
			return this.htmlApplicationCache;
		}

		public void setHtmlApplicationCache(boolean htmlApplicationCache) {
			this.htmlApplicationCache = htmlApplicationCache;
		}

		public boolean isCompressed() {
			return this.compressed;
		}

		public void setCompressed(boolean compressed) {
			this.compressed = compressed;
		}

		static Boolean getEnabled(boolean fixedEnabled, boolean contentEnabled, Boolean chainEnabled) {
			return (fixedEnabled || contentEnabled) ? Boolean.TRUE : chainEnabled;
		}

	}

	/**
	 * Strategies for extracting and embedding a resource version in its URL path.
	 */
	public static class Strategy {

		private final Fixed fixed = new Fixed();

		private final Content content = new Content();

		public Fixed getFixed() {
			return this.fixed;
		}

		public Content getContent() {
			return this.content;
		}

	}

	/**
	 * Version Strategy based on content hashing.
	 */
	public static class Content {

		/**
		 * Whether to enable the content Version Strategy.
		 */
		private boolean enabled;

		/**
		 * Comma-separated list of patterns to apply to the content Version Strategy.
		 */
		private String[] paths = new String[] { "/**" };

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String[] getPaths() {
			return this.paths;
		}

		public void setPaths(String[] paths) {
			this.paths = paths;
		}

	}

	/**
	 * Version Strategy based on a fixed version string.
	 */
	public static class Fixed {

		/**
		 * Whether to enable the fixed Version Strategy.
		 */
		private boolean enabled;

		/**
		 * Comma-separated list of patterns to apply to the fixed Version Strategy.
		 */
		private String[] paths = new String[] { "/**" };

		/**
		 * Version string to use for the fixed Version Strategy.
		 */
		private String version;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String[] getPaths() {
			return this.paths;
		}

		public void setPaths(String[] paths) {
			this.paths = paths;
		}

		public String getVersion() {
			return this.version;
		}

		public void setVersion(String version) {
			this.version = version;
		}

	}

	/**
	 * Cache configuration.
	 */
	public static class Cache {

		/**
		 * Cache period for the resources served by the resource handler. If a duration
		 * suffix is not specified, seconds will be used. Can be overridden by the
		 * 'spring.resources.cache.cachecontrol' properties.
		 */
		@DurationUnit(ChronoUnit.SECONDS)
		private Duration period;

		/**
		 * Cache control HTTP headers, only allows valid directive combinations. Overrides
		 * the 'spring.resources.cache.period' property.
		 */
		private final Cachecontrol cachecontrol = new Cachecontrol();

		public Duration getPeriod() {
			return this.period;
		}

		public void setPeriod(Duration period) {
			this.period = period;
		}

		public Cachecontrol getCachecontrol() {
			return this.cachecontrol;
		}

		/**
		 * Cache Control HTTP header configuration.
		 */
		public static class Cachecontrol {

			/**
			 * Maximum time the response should be cached, in seconds if no duration
			 * suffix is not specified.
			 */
			@DurationUnit(ChronoUnit.SECONDS)
			private Duration maxAge;

			/**
			 * Indicate that the cached response can be reused only if re-validated with
			 * the server.
			 */
			private Boolean noCache;

			/**
			 * Indicate to not cache the response in any case.
			 */
			private Boolean noStore;

			/**
			 * Indicate that once it has become stale, a cache must not use the response
			 * without re-validating it with the server.
			 */
			private Boolean mustRevalidate;

			/**
			 * Indicate intermediaries (caches and others) that they should not transform
			 * the response content.
			 */
			private Boolean noTransform;

			/**
			 * Indicate that any cache may store the response.
			 */
			private Boolean cachePublic;

			/**
			 * Indicate that the response message is intended for a single user and must
			 * not be stored by a shared cache.
			 */
			private Boolean cachePrivate;

			/**
			 * Same meaning as the "must-revalidate" directive, except that it does not
			 * apply to private caches.
			 */
			private Boolean proxyRevalidate;

			/**
			 * Maximum time the response can be served after it becomes stale, in seconds
			 * if no duration suffix is not specified.
			 */
			@DurationUnit(ChronoUnit.SECONDS)
			private Duration staleWhileRevalidate;

			/**
			 * Maximum time the response may be used when errors are encountered, in
			 * seconds if no duration suffix is not specified.
			 */
			@DurationUnit(ChronoUnit.SECONDS)
			private Duration staleIfError;

			/**
			 * Maximum time the response should be cached by shared caches, in seconds if
			 * no duration suffix is not specified.
			 */
			@DurationUnit(ChronoUnit.SECONDS)
			private Duration sMaxAge;

			public Duration getMaxAge() {
				return this.maxAge;
			}

			public void setMaxAge(Duration maxAge) {
				this.maxAge = maxAge;
			}

			public Boolean getNoCache() {
				return this.noCache;
			}

			public void setNoCache(Boolean noCache) {
				this.noCache = noCache;
			}

			public Boolean getNoStore() {
				return this.noStore;
			}

			public void setNoStore(Boolean noStore) {
				this.noStore = noStore;
			}

			public Boolean getMustRevalidate() {
				return this.mustRevalidate;
			}

			public void setMustRevalidate(Boolean mustRevalidate) {
				this.mustRevalidate = mustRevalidate;
			}

			public Boolean getNoTransform() {
				return this.noTransform;
			}

			public void setNoTransform(Boolean noTransform) {
				this.noTransform = noTransform;
			}

			public Boolean getCachePublic() {
				return this.cachePublic;
			}

			public void setCachePublic(Boolean cachePublic) {
				this.cachePublic = cachePublic;
			}

			public Boolean getCachePrivate() {
				return this.cachePrivate;
			}

			public void setCachePrivate(Boolean cachePrivate) {
				this.cachePrivate = cachePrivate;
			}

			public Boolean getProxyRevalidate() {
				return this.proxyRevalidate;
			}

			public void setProxyRevalidate(Boolean proxyRevalidate) {
				this.proxyRevalidate = proxyRevalidate;
			}

			public Duration getStaleWhileRevalidate() {
				return this.staleWhileRevalidate;
			}

			public void setStaleWhileRevalidate(Duration staleWhileRevalidate) {
				this.staleWhileRevalidate = staleWhileRevalidate;
			}

			public Duration getStaleIfError() {
				return this.staleIfError;
			}

			public void setStaleIfError(Duration staleIfError) {
				this.staleIfError = staleIfError;
			}

			public Duration getSMaxAge() {
				return this.sMaxAge;
			}

			public void setSMaxAge(Duration sMaxAge) {
				this.sMaxAge = sMaxAge;
			}

			public CacheControl toHttpCacheControl() {
				PropertyMapper map = PropertyMapper.get();
				CacheControl control = createCacheControl();
				map.from(this::getMustRevalidate).whenTrue().toCall(control::mustRevalidate);
				map.from(this::getNoTransform).whenTrue().toCall(control::noTransform);
				map.from(this::getCachePublic).whenTrue().toCall(control::cachePublic);
				map.from(this::getCachePrivate).whenTrue().toCall(control::cachePrivate);
				map.from(this::getProxyRevalidate).whenTrue().toCall(control::proxyRevalidate);
				map.from(this::getStaleWhileRevalidate).whenNonNull()
						.to((duration) -> control.staleWhileRevalidate(duration.getSeconds(), TimeUnit.SECONDS));
				map.from(this::getStaleIfError).whenNonNull()
						.to((duration) -> control.staleIfError(duration.getSeconds(), TimeUnit.SECONDS));
				map.from(this::getSMaxAge).whenNonNull()
						.to((duration) -> control.sMaxAge(duration.getSeconds(), TimeUnit.SECONDS));
				// check if cacheControl remained untouched
				if (control.getHeaderValue() == null) {
					return null;
				}
				return control;
			}

			private CacheControl createCacheControl() {
				if (Boolean.TRUE.equals(this.noStore)) {
					return CacheControl.noStore();
				}
				if (Boolean.TRUE.equals(this.noCache)) {
					return CacheControl.noCache();
				}
				if (this.maxAge != null) {
					return CacheControl.maxAge(this.maxAge.getSeconds(), TimeUnit.SECONDS);
				}
				return CacheControl.empty();
			}

		}

	}

}

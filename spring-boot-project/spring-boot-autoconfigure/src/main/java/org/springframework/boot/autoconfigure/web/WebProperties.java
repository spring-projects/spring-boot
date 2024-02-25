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

package org.springframework.boot.autoconfigure.web;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.http.CacheControl;

/**
 * {@link ConfigurationProperties Configuration properties} for general web concerns.
 *
 * @author Andy Wilkinson
 * @since 2.4.0
 */
@ConfigurationProperties("spring.web")
public class WebProperties {

	/**
	 * Locale to use. By default, this locale is overridden by the "Accept-Language"
	 * header.
	 */
	private Locale locale;

	/**
	 * Define how the locale should be resolved.
	 */
	private LocaleResolver localeResolver = LocaleResolver.ACCEPT_HEADER;

	private final Resources resources = new Resources();

	/**
     * Returns the locale of the WebProperties.
     *
     * @return the locale of the WebProperties
     */
    public Locale getLocale() {
		return this.locale;
	}

	/**
     * Sets the locale for the WebProperties.
     * 
     * @param locale the locale to be set
     */
    public void setLocale(Locale locale) {
		this.locale = locale;
	}

	/**
     * Returns the locale resolver used by the WebProperties class.
     *
     * @return the locale resolver used by the WebProperties class
     */
    public LocaleResolver getLocaleResolver() {
		return this.localeResolver;
	}

	/**
     * Sets the locale resolver for the web properties.
     * 
     * @param localeResolver the locale resolver to be set
     */
    public void setLocaleResolver(LocaleResolver localeResolver) {
		this.localeResolver = localeResolver;
	}

	/**
     * Returns the resources object associated with this WebProperties instance.
     *
     * @return the resources object
     */
    public Resources getResources() {
		return this.resources;
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

	/**
     * Resources class.
     */
    public static class Resources {

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

		private boolean customized = false;

		private final Chain chain = new Chain();

		private final Cache cache = new Cache();

		/**
         * Returns an array of static locations.
         *
         * @return an array of static locations
         */
        public String[] getStaticLocations() {
			return this.staticLocations;
		}

		/**
         * Sets the static locations for the Resources class.
         * 
         * @param staticLocations an array of static locations to be set
         * 
         * @return void
         */
        public void setStaticLocations(String[] staticLocations) {
			this.staticLocations = appendSlashIfNecessary(staticLocations);
			this.customized = true;
		}

		/**
         * Appends a slash to each location in the given array if it is not already present.
         * 
         * @param staticLocations the array of static locations to be normalized
         * @return the normalized array of static locations with a slash appended to each location if necessary
         */
        private String[] appendSlashIfNecessary(String[] staticLocations) {
			String[] normalized = new String[staticLocations.length];
			for (int i = 0; i < staticLocations.length; i++) {
				String location = staticLocations[i];
				normalized[i] = location.endsWith("/") ? location : location + "/";
			}
			return normalized;
		}

		/**
         * Returns a boolean value indicating whether to add mappings.
         * 
         * @return true if mappings should be added, false otherwise
         */
        public boolean isAddMappings() {
			return this.addMappings;
		}

		/**
         * Sets the value of the addMappings property.
         * 
         * @param addMappings the new value for the addMappings property
         */
        public void setAddMappings(boolean addMappings) {
			this.customized = true;
			this.addMappings = addMappings;
		}

		/**
         * Returns the chain object.
         * 
         * @return the chain object
         */
        public Chain getChain() {
			return this.chain;
		}

		/**
         * Returns the cache object.
         *
         * @return the cache object
         */
        public Cache getCache() {
			return this.cache;
		}

		/**
         * Checks if the resource has been customized.
         * 
         * @return true if the resource has been customized, false otherwise
         */
        public boolean hasBeenCustomized() {
			return this.customized || getChain().hasBeenCustomized() || getCache().hasBeenCustomized();
		}

		/**
		 * Configuration for the Spring Resource Handling chain.
		 */
		public static class Chain {

			boolean customized = false;

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
			 * Whether to enable resolution of already compressed resources (gzip,
			 * brotli). Checks for a resource name with the '.gz' or '.br' file
			 * extensions.
			 */
			private boolean compressed = false;

			private final Strategy strategy = new Strategy();

			/**
			 * Return whether the resource chain is enabled. Return {@code null} if no
			 * specific settings are present.
			 * @return whether the resource chain is enabled or {@code null} if no
			 * specified settings are present.
			 */
			public Boolean getEnabled() {
				return getEnabled(getStrategy().getFixed().isEnabled(), getStrategy().getContent().isEnabled(),
						this.enabled);
			}

			/**
             * Checks if the Chain object has been customized.
             * 
             * @return true if the Chain object has been customized, false otherwise.
             */
            private boolean hasBeenCustomized() {
				return this.customized || getStrategy().hasBeenCustomized();
			}

			/**
             * Sets the enabled status of the Chain.
             * 
             * @param enabled the enabled status to be set
             */
            public void setEnabled(boolean enabled) {
				this.enabled = enabled;
				this.customized = true;
			}

			/**
             * Returns a boolean value indicating whether the cache is enabled or not.
             *
             * @return true if the cache is enabled, false otherwise.
             */
            public boolean isCache() {
				return this.cache;
			}

			/**
             * Sets the cache flag for the Chain.
             * 
             * @param cache the boolean value indicating whether to enable caching or not
             * @return void
             */
            public void setCache(boolean cache) {
				this.cache = cache;
				this.customized = true;
			}

			/**
             * Returns the strategy used by the Chain object.
             * 
             * @return the strategy used by the Chain object
             */
            public Strategy getStrategy() {
				return this.strategy;
			}

			/**
             * Returns a boolean value indicating whether the Chain is compressed or not.
             * 
             * @return true if the Chain is compressed, false otherwise
             */
            public boolean isCompressed() {
				return this.compressed;
			}

			/**
             * Sets the compressed flag for the Chain.
             * 
             * @param compressed the value to set for the compressed flag
             */
            public void setCompressed(boolean compressed) {
				this.compressed = compressed;
				this.customized = true;
			}

			/**
             * Returns the enabled status based on the given parameters.
             * 
             * @param fixedEnabled  the fixed enabled status
             * @param contentEnabled  the content enabled status
             * @param chainEnabled  the chain enabled status
             * @return the enabled status
             */
            static Boolean getEnabled(boolean fixedEnabled, boolean contentEnabled, Boolean chainEnabled) {
				return (fixedEnabled || contentEnabled) ? Boolean.TRUE : chainEnabled;
			}

			/**
			 * Strategies for extracting and embedding a resource version in its URL path.
			 */
			public static class Strategy {

				private final Fixed fixed = new Fixed();

				private final Content content = new Content();

				/**
                 * Returns the Fixed object associated with this Strategy.
                 *
                 * @return the Fixed object associated with this Strategy
                 */
                public Fixed getFixed() {
					return this.fixed;
				}

				/**
                 * Returns the content of the Strategy.
                 *
                 * @return the content of the Strategy
                 */
                public Content getContent() {
					return this.content;
				}

				/**
                 * Checks if the strategy has been customized.
                 * 
                 * @return true if the strategy has been customized, false otherwise
                 */
                private boolean hasBeenCustomized() {
					return getFixed().hasBeenCustomized() || getContent().hasBeenCustomized();
				}

				/**
				 * Version Strategy based on content hashing.
				 */
				public static class Content {

					private boolean customized = false;

					/**
					 * Whether to enable the content Version Strategy.
					 */
					private boolean enabled;

					/**
					 * Comma-separated list of patterns to apply to the content Version
					 * Strategy.
					 */
					private String[] paths = new String[] { "/**" };

					/**
                     * Returns the current status of the content's enabled flag.
                     *
                     * @return true if the content is enabled, false otherwise.
                     */
                    public boolean isEnabled() {
						return this.enabled;
					}

					/**
                     * Sets the enabled status of the content.
                     * 
                     * @param enabled the enabled status to be set
                     */
                    public void setEnabled(boolean enabled) {
						this.customized = true;
						this.enabled = enabled;
					}

					/**
                     * Returns an array of paths.
                     *
                     * @return an array of paths
                     */
                    public String[] getPaths() {
						return this.paths;
					}

					/**
                     * Sets the paths for the content.
                     * 
                     * @param paths an array of strings representing the paths
                     */
                    public void setPaths(String[] paths) {
						this.customized = true;
						this.paths = paths;
					}

					/**
                     * Returns a boolean value indicating whether the content has been customized.
                     * 
                     * @return true if the content has been customized, false otherwise
                     */
                    private boolean hasBeenCustomized() {
						return this.customized;
					}

				}

				/**
				 * Version Strategy based on a fixed version string.
				 */
				public static class Fixed {

					private boolean customized = false;

					/**
					 * Whether to enable the fixed Version Strategy.
					 */
					private boolean enabled;

					/**
					 * Comma-separated list of patterns to apply to the fixed Version
					 * Strategy.
					 */
					private String[] paths = new String[] { "/**" };

					/**
					 * Version string to use for the fixed Version Strategy.
					 */
					private String version;

					/**
                     * Returns the current status of the enabled flag.
                     *
                     * @return true if the enabled flag is set, false otherwise.
                     */
                    public boolean isEnabled() {
						return this.enabled;
					}

					/**
                     * Sets the enabled status of the Fixed object.
                     * 
                     * @param enabled the new enabled status
                     */
                    public void setEnabled(boolean enabled) {
						this.customized = true;
						this.enabled = enabled;
					}

					/**
                     * Returns an array of paths.
                     *
                     * @return an array of paths
                     */
                    public String[] getPaths() {
						return this.paths;
					}

					/**
                     * Sets the paths for the Fixed class.
                     * 
                     * @param paths an array of strings representing the paths to be set
                     */
                    public void setPaths(String[] paths) {
						this.customized = true;
						this.paths = paths;
					}

					/**
                     * Returns the version of the Fixed class.
                     *
                     * @return the version of the Fixed class
                     */
                    public String getVersion() {
						return this.version;
					}

					/**
                     * Sets the version of the Fixed class.
                     * 
                     * @param version the version to be set
                     */
                    public void setVersion(String version) {
						this.customized = true;
						this.version = version;
					}

					/**
                     * Returns a boolean value indicating whether the object has been customized.
                     *
                     * @return true if the object has been customized, false otherwise.
                     */
                    private boolean hasBeenCustomized() {
						return this.customized;
					}

				}

			}

		}

		/**
		 * Cache configuration.
		 */
		public static class Cache {

			private boolean customized = false;

			/**
			 * Cache period for the resources served by the resource handler. If a
			 * duration suffix is not specified, seconds will be used. Can be overridden
			 * by the 'spring.web.resources.cache.cachecontrol' properties.
			 */
			@DurationUnit(ChronoUnit.SECONDS)
			private Duration period;

			/**
			 * Cache control HTTP headers, only allows valid directive combinations.
			 * Overrides the 'spring.web.resources.cache.period' property.
			 */
			private final Cachecontrol cachecontrol = new Cachecontrol();

			/**
			 * Whether we should use the "lastModified" metadata of the files in HTTP
			 * caching headers.
			 */
			private boolean useLastModified = true;

			/**
             * Returns the period of time for which the cache is valid.
             *
             * @return the period of time for which the cache is valid
             */
            public Duration getPeriod() {
				return this.period;
			}

			/**
             * Sets the period for cache expiration.
             * 
             * @param period the duration for cache expiration
             */
            public void setPeriod(Duration period) {
				this.customized = true;
				this.period = period;
			}

			/**
             * Returns the Cachecontrol object associated with this Cache.
             *
             * @return the Cachecontrol object associated with this Cache
             */
            public Cachecontrol getCachecontrol() {
				return this.cachecontrol;
			}

			/**
             * Returns a boolean value indicating whether the cache should use the last modified timestamp.
             *
             * @return true if the cache should use the last modified timestamp, false otherwise
             */
            public boolean isUseLastModified() {
				return this.useLastModified;
			}

			/**
             * Sets whether to use the last modified timestamp for caching.
             * 
             * @param useLastModified true to use the last modified timestamp for caching, false otherwise
             */
            public void setUseLastModified(boolean useLastModified) {
				this.useLastModified = useLastModified;
			}

			/**
             * Checks if the cache has been customized.
             * 
             * @return true if the cache has been customized, false otherwise
             */
            private boolean hasBeenCustomized() {
				return this.customized || getCachecontrol().hasBeenCustomized();
			}

			/**
			 * Cache Control HTTP header configuration.
			 */
			public static class Cachecontrol {

				private boolean customized = false;

				/**
				 * Maximum time the response should be cached, in seconds if no duration
				 * suffix is not specified.
				 */
				@DurationUnit(ChronoUnit.SECONDS)
				private Duration maxAge;

				/**
				 * Indicate that the cached response can be reused only if re-validated
				 * with the server.
				 */
				private Boolean noCache;

				/**
				 * Indicate to not cache the response in any case.
				 */
				private Boolean noStore;

				/**
				 * Indicate that once it has become stale, a cache must not use the
				 * response without re-validating it with the server.
				 */
				private Boolean mustRevalidate;

				/**
				 * Indicate intermediaries (caches and others) that they should not
				 * transform the response content.
				 */
				private Boolean noTransform;

				/**
				 * Indicate that any cache may store the response.
				 */
				private Boolean cachePublic;

				/**
				 * Indicate that the response message is intended for a single user and
				 * must not be stored by a shared cache.
				 */
				private Boolean cachePrivate;

				/**
				 * Same meaning as the "must-revalidate" directive, except that it does
				 * not apply to private caches.
				 */
				private Boolean proxyRevalidate;

				/**
				 * Maximum time the response can be served after it becomes stale, in
				 * seconds if no duration suffix is not specified.
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
				 * Maximum time the response should be cached by shared caches, in seconds
				 * if no duration suffix is not specified.
				 */
				@DurationUnit(ChronoUnit.SECONDS)
				private Duration sMaxAge;

				/**
                 * Returns the maximum age of the cache control.
                 *
                 * @return the maximum age of the cache control
                 */
                public Duration getMaxAge() {
					return this.maxAge;
				}

				/**
                 * Sets the maximum age for the cache control.
                 * 
                 * @param maxAge the maximum age duration to be set
                 */
                public void setMaxAge(Duration maxAge) {
					this.customized = true;
					this.maxAge = maxAge;
				}

				/**
                 * Returns the value of the noCache property.
                 * 
                 * @return the value of the noCache property
                 */
                public Boolean getNoCache() {
					return this.noCache;
				}

				/**
                 * Sets the value of the noCache property.
                 * 
                 * @param noCache the new value for the noCache property
                 */
                public void setNoCache(Boolean noCache) {
					this.customized = true;
					this.noCache = noCache;
				}

				/**
                 * Returns the value of the noStore property.
                 * 
                 * @return the value of the noStore property
                 */
                public Boolean getNoStore() {
					return this.noStore;
				}

				/**
                 * Sets the value of the noStore property.
                 * 
                 * @param noStore the new value for the noStore property
                 */
                public void setNoStore(Boolean noStore) {
					this.customized = true;
					this.noStore = noStore;
				}

				/**
                 * Returns the value of the mustRevalidate property.
                 * 
                 * @return the value of the mustRevalidate property
                 */
                public Boolean getMustRevalidate() {
					return this.mustRevalidate;
				}

				/**
                 * Sets the mustRevalidate flag for the Cachecontrol.
                 * 
                 * @param mustRevalidate the value to set for the mustRevalidate flag
                 */
                public void setMustRevalidate(Boolean mustRevalidate) {
					this.customized = true;
					this.mustRevalidate = mustRevalidate;
				}

				/**
                 * Returns the value of the noTransform flag.
                 * 
                 * @return true if the noTransform flag is set, false otherwise.
                 */
                public Boolean getNoTransform() {
					return this.noTransform;
				}

				/**
                 * Sets the value of the noTransform property.
                 * 
                 * @param noTransform the new value for the noTransform property
                 */
                public void setNoTransform(Boolean noTransform) {
					this.customized = true;
					this.noTransform = noTransform;
				}

				/**
                 * Returns the value of the cachePublic property.
                 *
                 * @return the value of the cachePublic property
                 */
                public Boolean getCachePublic() {
					return this.cachePublic;
				}

				/**
                 * Sets the cachePublic flag for the Cachecontrol.
                 * 
                 * @param cachePublic the value to set the cachePublic flag to
                 */
                public void setCachePublic(Boolean cachePublic) {
					this.customized = true;
					this.cachePublic = cachePublic;
				}

				/**
                 * Returns the value of the cachePrivate property.
                 * 
                 * @return the value of the cachePrivate property
                 */
                public Boolean getCachePrivate() {
					return this.cachePrivate;
				}

				/**
                 * Sets the cachePrivate flag.
                 * 
                 * @param cachePrivate the value to set for the cachePrivate flag
                 */
                public void setCachePrivate(Boolean cachePrivate) {
					this.customized = true;
					this.cachePrivate = cachePrivate;
				}

				/**
                 * Returns the value of the proxyRevalidate property.
                 * 
                 * @return the value of the proxyRevalidate property
                 */
                public Boolean getProxyRevalidate() {
					return this.proxyRevalidate;
				}

				/**
                 * Sets the value of the proxyRevalidate flag.
                 * 
                 * @param proxyRevalidate the new value for the proxyRevalidate flag
                 */
                public void setProxyRevalidate(Boolean proxyRevalidate) {
					this.customized = true;
					this.proxyRevalidate = proxyRevalidate;
				}

				/**
                 * Returns the duration of time that a cached response can be considered stale while it is being revalidated.
                 *
                 * @return the duration of time for stale while revalidate
                 */
                public Duration getStaleWhileRevalidate() {
					return this.staleWhileRevalidate;
				}

				/**
                 * Sets the duration for which a response can be considered stale while revalidating.
                 * 
                 * @param staleWhileRevalidate the duration for which a response can be considered stale while revalidating
                 */
                public void setStaleWhileRevalidate(Duration staleWhileRevalidate) {
					this.customized = true;
					this.staleWhileRevalidate = staleWhileRevalidate;
				}

				/**
                 * Returns the duration for which a cached response can be considered stale if an error occurs.
                 *
                 * @return the duration for which a cached response can be considered stale if an error occurs
                 */
                public Duration getStaleIfError() {
					return this.staleIfError;
				}

				/**
                 * Sets the duration for which a cache entry should be considered stale if an error occurs.
                 * 
                 * @param staleIfError the duration for which a cache entry should be considered stale if an error occurs
                 */
                public void setStaleIfError(Duration staleIfError) {
					this.customized = true;
					this.staleIfError = staleIfError;
				}

				/**
                 * Returns the maximum age of a shared cache entry in seconds.
                 *
                 * @return the maximum age of a shared cache entry in seconds
                 */
                public Duration getSMaxAge() {
					return this.sMaxAge;
				}

				/**
                 * Sets the maximum age of a shared cache entry in seconds.
                 * 
                 * @param sMaxAge the maximum age of a shared cache entry
                 */
                public void setSMaxAge(Duration sMaxAge) {
					this.customized = true;
					this.sMaxAge = sMaxAge;
				}

				/**
                 * Converts the current instance of CacheControl to an instance of HttpCacheControl.
                 * 
                 * @return The converted HttpCacheControl instance.
                 */
                public CacheControl toHttpCacheControl() {
					PropertyMapper map = PropertyMapper.get();
					CacheControl control = createCacheControl();
					map.from(this::getMustRevalidate).whenTrue().toCall(control::mustRevalidate);
					map.from(this::getNoTransform).whenTrue().toCall(control::noTransform);
					map.from(this::getCachePublic).whenTrue().toCall(control::cachePublic);
					map.from(this::getCachePrivate).whenTrue().toCall(control::cachePrivate);
					map.from(this::getProxyRevalidate).whenTrue().toCall(control::proxyRevalidate);
					map.from(this::getStaleWhileRevalidate)
						.whenNonNull()
						.to((duration) -> control.staleWhileRevalidate(duration.getSeconds(), TimeUnit.SECONDS));
					map.from(this::getStaleIfError)
						.whenNonNull()
						.to((duration) -> control.staleIfError(duration.getSeconds(), TimeUnit.SECONDS));
					map.from(this::getSMaxAge)
						.whenNonNull()
						.to((duration) -> control.sMaxAge(duration.getSeconds(), TimeUnit.SECONDS));
					// check if cacheControl remained untouched
					if (control.getHeaderValue() == null) {
						return null;
					}
					return control;
				}

				/**
                 * Creates a CacheControl object based on the provided configuration.
                 * 
                 * @return The created CacheControl object.
                 */
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

				/**
                 * Returns a boolean value indicating whether the cache control has been customized.
                 *
                 * @return true if the cache control has been customized, false otherwise.
                 */
                private boolean hasBeenCustomized() {
					return this.customized;
				}

			}

		}

	}

}

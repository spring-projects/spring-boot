/*
 * Copyright 2012-2017 the original author or authors.
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

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import javax.annotation.PostConstruct;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.context.properties.bind.convert.DurationUnit;
import org.springframework.http.CacheControl;
import org.springframework.util.Assert;

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

	private static final String[] CLASSPATH_RESOURCE_LOCATIONS = {
			"classpath:/META-INF/resources/", "classpath:/resources/",
			"classpath:/static/", "classpath:/public/"};

	/**
	 * Locations of static resources. Defaults to classpath:[/META-INF/resources/,
	 * /resources/, /static/, /public/].
	 */
	private String[] staticLocations = CLASSPATH_RESOURCE_LOCATIONS;

	/**
	 * Cache period for the resources served by the resource handler. If a duration suffix
	 * is not specified, seconds will be used.
	 */
	@DurationUnit(ChronoUnit.SECONDS)
	private Duration cachePeriod;

	/**
	 * Cache control headers. Either {@link #cachePeriod} or {@link #cacheControl} can be set.
	 */
	@NestedConfigurationProperty
	private CacheControlProperties cacheControl;

	/**
	 * Enable default resource handling.
	 */
	private boolean addMappings = true;

	private final Chain chain = new Chain();

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
			normalized[i] = (location.endsWith("/") ? location : location + "/");
		}
		return normalized;
	}

	public Duration getCachePeriod() {
		return this.cachePeriod;
	}

	public void setCachePeriod(Duration cachePeriod) {
		this.cachePeriod = cachePeriod;
	}

	public CacheControlProperties getCacheControl() {
		return this.cacheControl;
	}

	public void setCacheControl(CacheControlProperties cacheControl) {
		this.cacheControl = cacheControl;
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

	public CacheControl createCacheControl() {
		if (this.cachePeriod != null) {
			return CacheControl.maxAge(this.cachePeriod.getSeconds(), TimeUnit.SECONDS);
		}
		if (this.cacheControl != null) {
			return this.cacheControl.transformToHttpSpringCacheControl();
		}
		return null;
	}

	@PostConstruct
	public void checkIncompatibleCacheOptions() {
		Assert.state(this.cachePeriod == null || this.cacheControl == null,
				"Only one of cache-period or cache-control may be set.");
		if (this.cacheControl != null) {
			if (this.cacheControl.getMaxAge() != null) {
				Assert.state(!Boolean.TRUE.equals(this.cacheControl.getNoCache()), "no-cache may not be set if max-age is set.");
				Assert.state(!Boolean.TRUE.equals(this.cacheControl.getNoStore()), "no-store may not be set if max-age is set.");
			}
			if (this.cacheControl.getNoCache() != null) {
				Assert.state(this.cacheControl.getMaxAge() == null, "max-age may not be set if no-cache is set.");
				Assert.state(!Boolean.TRUE.equals(this.cacheControl.getNoStore()), "no-store may not be set if no-cache is set.");
			}
			if (this.cacheControl.getNoStore() != null) {
				Assert.state(this.cacheControl.getMaxAge() == null, "max-age may not be set if no-store is set.");
				Assert.state(!Boolean.TRUE.equals(this.cacheControl.getNoCache()), "no-cache may not be set if no-store is set.");
			}
		}
	}

	/**
	 * Configuration for the Spring Resource Handling chain.
	 */
	public static class Chain {

		/**
		 * Enable the Spring Resource Handling chain. Disabled by default unless at least
		 * one strategy has been enabled.
		 */
		private Boolean enabled;

		/**
		 * Enable caching in the Resource chain.
		 */
		private boolean cache = true;

		/**
		 * Enable HTML5 application cache manifest rewriting.
		 */
		private boolean htmlApplicationCache = false;

		/**
		 * Enable resolution of already gzipped resources. Checks for a resource name
		 * variant with the "*.gz" extension.
		 */
		private boolean gzipped = false;

		private final Strategy strategy = new Strategy();

		/**
		 * Return whether the resource chain is enabled. Return {@code null} if no
		 * specific settings are present.
		 *
		 * @return whether the resource chain is enabled or {@code null} if no specified settings are
		 * present.
		 */
		public Boolean getEnabled() {
			return getEnabled(getStrategy().getFixed().isEnabled(),
					getStrategy().getContent().isEnabled(), this.enabled);
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

		public boolean isHtmlApplicationCache() {
			return this.htmlApplicationCache;
		}

		public void setHtmlApplicationCache(boolean htmlApplicationCache) {
			this.htmlApplicationCache = htmlApplicationCache;
		}

		public boolean isGzipped() {
			return this.gzipped;
		}

		public void setGzipped(boolean gzipped) {
			this.gzipped = gzipped;
		}

		static Boolean getEnabled(boolean fixedEnabled, boolean contentEnabled,
				Boolean chainEnabled) {
			return (fixedEnabled || contentEnabled ? Boolean.TRUE : chainEnabled);
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
		 * Enable the content Version Strategy.
		 */
		private boolean enabled;

		/**
		 * Comma-separated list of patterns to apply to the Version Strategy.
		 */
		private String[] paths = new String[]{"/**"};

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
		 * Enable the fixed Version Strategy.
		 */
		private boolean enabled;

		/**
		 * Comma-separated list of patterns to apply to the Version Strategy.
		 */
		private String[] paths = new String[]{"/**"};

		/**
		 * Version string to use for the Version Strategy.
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
	 * Configuration for the Cache Control header.
	 */

	public static class CacheControlProperties {

		private Long maxAge;

		private Boolean noCache;

		private Boolean noStore;

		private Boolean mustRevalidate;

		private Boolean noTransform;

		private Boolean cachePublic;

		private Boolean cachePrivate;

		private Boolean proxyRevalidate;

		private Long staleWhileRevalidate;

		private Long staleIfError;

		private Long sMaxAge;

		public Long getMaxAge() {
			return this.maxAge;
		}

		public void setMaxAge(Long maxAge) {
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

		public Long getStaleWhileRevalidate() {
			return this.staleWhileRevalidate;
		}

		public void setStaleWhileRevalidate(Long staleWhileRevalidate) {
			this.staleWhileRevalidate = staleWhileRevalidate;
		}

		public Long getStaleIfError() {
			return this.staleIfError;
		}

		public void setStaleIfError(Long staleIfError) {
			this.staleIfError = staleIfError;
		}

		public Long getsMaxAge() {
			return this.sMaxAge;
		}

		public void setsMaxAge(Long sMaxAge) {
			this.sMaxAge = sMaxAge;
		}

		CacheControl transformToHttpSpringCacheControl() {
			CacheControl httpSpringCacheControl = initCacheControl();
			httpSpringCacheControl = setFlags(httpSpringCacheControl);
			httpSpringCacheControl = setTimes(httpSpringCacheControl);
			return httpSpringCacheControl;
		}

		private CacheControl initCacheControl() {
			if (this.maxAge != null) {
				return CacheControl.maxAge(this.maxAge, TimeUnit.SECONDS);
			}
			if (Boolean.TRUE.equals(this.noCache)) {
				return CacheControl.noCache();
			}
			if (Boolean.TRUE.equals(this.noStore)) {
				return CacheControl.noStore();
			}
			return CacheControl.empty();
		}

		private CacheControl setFlags(CacheControl cacheControl) {
			cacheControl = setBoolean(this.mustRevalidate, cacheControl::mustRevalidate,
					cacheControl);
			cacheControl = setBoolean(this.noTransform, cacheControl::noTransform,
					cacheControl);
			cacheControl = setBoolean(this.cachePublic, cacheControl::cachePublic,
					cacheControl);
			cacheControl = setBoolean(this.cachePrivate, cacheControl::cachePrivate,
					cacheControl);
			cacheControl = setBoolean(this.proxyRevalidate, cacheControl::proxyRevalidate,
					cacheControl);
			return cacheControl;
		}

		private static CacheControl setBoolean(Boolean value,
				Supplier<CacheControl> setter, CacheControl cacheControl) {
			if (Boolean.TRUE.equals(value)) {
				return setter.get();
			}
			return cacheControl;
		}

		private CacheControl setTimes(CacheControl cacheControl) {
			cacheControl = setLong(this.staleWhileRevalidate,
					cacheControl::staleWhileRevalidate, cacheControl);
			cacheControl = setLong(this.staleIfError, cacheControl::staleIfError,
					cacheControl);
			cacheControl = setLong(this.sMaxAge, cacheControl::sMaxAge, cacheControl);
			return cacheControl;
		}

		private static CacheControl setLong(Long value,
				BiFunction<Long, TimeUnit, CacheControl> setter,
				CacheControl cacheControl) {
			if (value != null) {
				return setter.apply(value, TimeUnit.SECONDS);
			}
			return cacheControl;
		}

	}


}

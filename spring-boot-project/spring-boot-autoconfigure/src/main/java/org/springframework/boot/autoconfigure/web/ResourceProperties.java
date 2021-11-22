/*
 * Copyright 2012-2021 the original author or authors.
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

import org.springframework.boot.autoconfigure.web.WebProperties.Resources;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;

/**
 * Properties used to configure resource handling.
 *
 * @author Phillip Webb
 * @author Brian Clozel
 * @author Dave Syer
 * @author Venil Noronha
 * @author Kristine Jetzke
 * @since 1.1.0
 * @deprecated since 2.4.0 for removal in 2.6.0 in favor of
 * {@link WebProperties.Resources} accessed through {@link WebProperties} and
 * {@link WebProperties#getResources() getResources()}
 */
@Deprecated
@ConfigurationProperties(prefix = "spring.resources", ignoreUnknownFields = false)
public class ResourceProperties extends Resources {

	private final Chain chain = new Chain();

	private final Cache cache = new Cache();

	@Override
	@DeprecatedConfigurationProperty(replacement = "spring.web.resources.static-locations")
	public String[] getStaticLocations() {
		return super.getStaticLocations();
	}

	@Override
	@DeprecatedConfigurationProperty(replacement = "spring.web.resources.add-mappings")
	public boolean isAddMappings() {
		return super.isAddMappings();
	}

	@Override
	public Chain getChain() {
		return this.chain;
	}

	@Override
	public Cache getCache() {
		return this.cache;
	}

	@Deprecated
	public static class Chain extends Resources.Chain {

		private final org.springframework.boot.autoconfigure.web.ResourceProperties.Strategy strategy = new org.springframework.boot.autoconfigure.web.ResourceProperties.Strategy();

		/**
		 * Whether to enable HTML5 application cache manifest rewriting.
		 */
		private boolean htmlApplicationCache = false;

		@Override
		@DeprecatedConfigurationProperty(replacement = "spring.web.resources.chain.enabled")
		public Boolean getEnabled() {
			return super.getEnabled();
		}

		@Override
		@DeprecatedConfigurationProperty(replacement = "spring.web.resources.chain.cache")
		public boolean isCache() {
			return super.isCache();
		}

		@DeprecatedConfigurationProperty(reason = "The appcache manifest feature is being removed from browsers.")
		public boolean isHtmlApplicationCache() {
			return this.htmlApplicationCache;
		}

		public void setHtmlApplicationCache(boolean htmlApplicationCache) {
			this.htmlApplicationCache = htmlApplicationCache;
			this.customized = true;
		}

		@Override
		@DeprecatedConfigurationProperty(replacement = "spring.web.resources.chain.compressed")
		public boolean isCompressed() {
			return super.isCompressed();
		}

		@Override
		public org.springframework.boot.autoconfigure.web.ResourceProperties.Strategy getStrategy() {
			return this.strategy;
		}

	}

	/**
	 * Strategies for extracting and embedding a resource version in its URL path.
	 */
	@Deprecated
	public static class Strategy extends Resources.Chain.Strategy {

		private final org.springframework.boot.autoconfigure.web.ResourceProperties.Fixed fixed = new org.springframework.boot.autoconfigure.web.ResourceProperties.Fixed();

		private final org.springframework.boot.autoconfigure.web.ResourceProperties.Content content = new org.springframework.boot.autoconfigure.web.ResourceProperties.Content();

		@Override
		public org.springframework.boot.autoconfigure.web.ResourceProperties.Fixed getFixed() {
			return this.fixed;
		}

		@Override
		public org.springframework.boot.autoconfigure.web.ResourceProperties.Content getContent() {
			return this.content;
		}

	}

	/**
	 * Version Strategy based on content hashing.
	 */
	@Deprecated
	public static class Content extends Resources.Chain.Strategy.Content {

		@Override
		@DeprecatedConfigurationProperty(replacement = "spring.web.resources.chain.strategy.content.enabled")
		public boolean isEnabled() {
			return super.isEnabled();
		}

		@Override
		@DeprecatedConfigurationProperty(replacement = "spring.web.resources.chain.strategy.content.paths")
		public String[] getPaths() {
			return super.getPaths();
		}

	}

	/**
	 * Version Strategy based on a fixed version string.
	 */
	@Deprecated
	public static class Fixed extends Resources.Chain.Strategy.Fixed {

		@Override
		@DeprecatedConfigurationProperty(replacement = "spring.web.resources.chain.strategy.fixed.enabled")
		public boolean isEnabled() {
			return super.isEnabled();
		}

		@Override
		@DeprecatedConfigurationProperty(replacement = "spring.web.resources.chain.strategy.fixed.paths")
		public String[] getPaths() {
			return super.getPaths();
		}

		@Override
		@DeprecatedConfigurationProperty(replacement = "spring.web.resources.chain.strategy.fixed.version")
		public String getVersion() {
			return super.getVersion();
		}

	}

	/**
	 * Cache configuration.
	 */
	@Deprecated
	public static class Cache extends Resources.Cache {

		private final Cachecontrol cachecontrol = new Cachecontrol();

		@Override
		@DeprecatedConfigurationProperty(replacement = "spring.web.resources.cache.period")
		public Duration getPeriod() {
			return super.getPeriod();
		}

		@Override
		public Cachecontrol getCachecontrol() {
			return this.cachecontrol;
		}

		@Override
		@DeprecatedConfigurationProperty(replacement = "spring.web.resources.cache.use-last-modified")
		public boolean isUseLastModified() {
			return super.isUseLastModified();
		}

		/**
		 * Cache Control HTTP header configuration.
		 */
		@Deprecated
		public static class Cachecontrol extends Resources.Cache.Cachecontrol {

			@Override
			@DeprecatedConfigurationProperty(replacement = "spring.web.resources.cache.cachecontrol.max-age")
			public Duration getMaxAge() {
				return super.getMaxAge();
			}

			@Override
			@DeprecatedConfigurationProperty(replacement = "spring.web.resources.cache.cachecontrol.no-cache")
			public Boolean getNoCache() {
				return super.getNoCache();
			}

			@Override
			@DeprecatedConfigurationProperty(replacement = "spring.web.resources.cache.cachecontrol.no-store")
			public Boolean getNoStore() {
				return super.getNoStore();
			}

			@Override
			@DeprecatedConfigurationProperty(replacement = "spring.web.resources.cache.cachecontrol.must-revalidate")
			public Boolean getMustRevalidate() {
				return super.getMustRevalidate();
			}

			@Override
			@DeprecatedConfigurationProperty(replacement = "spring.web.resources.cache.cachecontrol.no-transform")
			public Boolean getNoTransform() {
				return super.getNoTransform();
			}

			@Override
			@DeprecatedConfigurationProperty(replacement = "spring.web.resources.cache.cachecontrol.cache-public")
			public Boolean getCachePublic() {
				return super.getCachePublic();
			}

			@Override
			@DeprecatedConfigurationProperty(replacement = "spring.web.resources.cache.cachecontrol.cache-private")
			public Boolean getCachePrivate() {
				return super.getCachePrivate();
			}

			@Override
			@DeprecatedConfigurationProperty(replacement = "spring.web.resources.cache.cachecontrol.proxy-revalidate")
			public Boolean getProxyRevalidate() {
				return super.getProxyRevalidate();
			}

			@Override
			@DeprecatedConfigurationProperty(
					replacement = "spring.web.resources.cache.cachecontrol.stale-while-revalidate")
			public Duration getStaleWhileRevalidate() {
				return super.getStaleWhileRevalidate();
			}

			@Override
			@DeprecatedConfigurationProperty(replacement = "spring.web.resources.cache.cachecontrol.stale-if-error")
			public Duration getStaleIfError() {
				return super.getStaleIfError();
			}

			@Override
			@DeprecatedConfigurationProperty(replacement = "spring.web.resources.cache.cachecontrol.s-max-age")
			public Duration getSMaxAge() {
				return super.getSMaxAge();
			}

		}

	}

}

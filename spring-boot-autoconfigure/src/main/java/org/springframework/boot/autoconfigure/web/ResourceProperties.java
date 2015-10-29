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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * Properties used to configure resource handling.
 *
 * @author Phillip Webb
 * @author Brian Clozel
 * @author Dave Syer
 * @since 1.1.0
 */
@ConfigurationProperties(prefix = "spring.resources", ignoreUnknownFields = false)
public class ResourceProperties implements ResourceLoaderAware {

	private static final String[] SERVLET_RESOURCE_LOCATIONS = { "/" };

	private static final String[] CLASSPATH_RESOURCE_LOCATIONS = {
			"classpath:/META-INF/resources/", "classpath:/resources/",
			"classpath:/static/", "classpath:/public/" };

	private static final String[] RESOURCE_LOCATIONS;

	static {
		RESOURCE_LOCATIONS = new String[CLASSPATH_RESOURCE_LOCATIONS.length
				+ SERVLET_RESOURCE_LOCATIONS.length];
		System.arraycopy(SERVLET_RESOURCE_LOCATIONS, 0, RESOURCE_LOCATIONS, 0,
				SERVLET_RESOURCE_LOCATIONS.length);
		System.arraycopy(CLASSPATH_RESOURCE_LOCATIONS, 0, RESOURCE_LOCATIONS,
				SERVLET_RESOURCE_LOCATIONS.length, CLASSPATH_RESOURCE_LOCATIONS.length);
	}

	/**
	 * Locations of static resources. Defaults to classpath:[/META-INF/resources/,
	 * /resources/, /static/, /public/] plus context:/ (the root of the servlet context).
	 */
	private String[] staticLocations = RESOURCE_LOCATIONS;

	/**
	 * Cache period for the resources served by the resource handler, in seconds.
	 */
	private Integer cachePeriod;

	/**
	 * Enable default resource handling.
	 */
	private boolean addMappings = true;

	private final Chain chain = new Chain();

	private ResourceLoader resourceLoader;

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	public String[] getStaticLocations() {
		return this.staticLocations;
	}

	public void setStaticLocations(String[] staticLocations) {
		this.staticLocations = staticLocations;
	}

	public Resource getWelcomePage() {
		for (String location : getStaticWelcomePageLocations()) {
			Resource resource = this.resourceLoader.getResource(location);
			try {
				if (resource.exists()) {
					resource.getURL();
					return resource;
				}
			}
			catch (Exception ex) {
				// Ignore
			}
		}
		return null;
	}

	private String[] getStaticWelcomePageLocations() {
		String[] result = new String[this.staticLocations.length];
		for (int i = 0; i < result.length; i++) {
			String location = this.staticLocations[i];
			if (!location.endsWith("/")) {
				location = location + "/";
			}
			result[i] = location + "index.html";
		}
		return result;
	}

	List<Resource> getFaviconLocations() {
		List<Resource> locations = new ArrayList<Resource>(
				CLASSPATH_RESOURCE_LOCATIONS.length + 1);
		if (this.resourceLoader != null) {
			for (String location : CLASSPATH_RESOURCE_LOCATIONS) {
				locations.add(this.resourceLoader.getResource(location));
			}
		}
		locations.add(new ClassPathResource("/"));
		return Collections.unmodifiableList(locations);
	}

	public Integer getCachePeriod() {
		return this.cachePeriod;
	}

	public void setCachePeriod(Integer cachePeriod) {
		this.cachePeriod = cachePeriod;
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

		@NestedConfigurationProperty
		private final Strategy strategy = new Strategy();

		public Boolean getEnabled() {
			return Boolean.TRUE.equals(this.enabled)
					|| getStrategy().getFixed().isEnabled()
					|| getStrategy().getContent().isEnabled();
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

	}

	/**
	 * Strategies for extracting and embedding a resource version in its URL path.
	 */
	public static class Strategy {

		@NestedConfigurationProperty
		private final Fixed fixed = new Fixed();

		@NestedConfigurationProperty
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
		 * Enable the fixed Version Strategy.
		 */
		private boolean enabled;

		/**
		 * Comma-separated list of patterns to apply to the Version Strategy.
		 */
		private String[] paths;

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

}

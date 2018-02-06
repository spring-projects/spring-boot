/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.ansi.AnsiPropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

/**
 * Banner implementation that prints from a source text {@link Resource}.
 *
 * @author Phillip Webb
 * @author Vedran Pavic
 * @since 1.2.0
 */
public class ResourceBanner implements Banner {

	private static final Log logger = LogFactory.getLog(ResourceBanner.class);

	private Resource bannerResource;
	private Resource gitPropertiesResource;
	private Resource buildInfoPropertiesResource;

	public ResourceBanner(Resource bannerResource, Resource gitPropertiesResource, Resource buildInfoPropertiesResource) {
		this(bannerResource);
		this.gitPropertiesResource = gitPropertiesResource;
		this.buildInfoPropertiesResource = buildInfoPropertiesResource;
	}

	public ResourceBanner(Resource bannerResource) {
		Assert.notNull(bannerResource, "Banner resource must not be null");
		Assert.isTrue(bannerResource.exists(), "Banner resource must exist");
		this.bannerResource = bannerResource;
	}

	@Override
	public void printBanner(Environment environment, Class<?> sourceClass,
			PrintStream out) {
		try {
			String banner = resourceToString(environment.getProperty("banner.charset", Charset.class,
				Charset.forName("UTF-8")), this.bannerResource);

			for (PropertyResolver resolver : getPropertyResolvers(environment,
					sourceClass)) {
				banner = resolver.resolvePlaceholders(banner);
			}
			out.println(banner);
		}
		catch (Exception ex) {
			logger.warn("Banner not printable: " + this.bannerResource + " (" + ex.getClass()
					+ ": '" + ex.getMessage() + "')", ex);
		}
	}

	private String resourceToString(Charset charset, Resource resource) throws IOException {
		return StreamUtils.copyToString(resource.getInputStream(),
				charset);
	}

	protected List<PropertyResolver> getPropertyResolvers(Environment environment,
			Class<?> sourceClass) {
		List<PropertyResolver> resolvers = new ArrayList<PropertyResolver>();
		resolvers.add(environment);
		resolvers.add(getVersionResolver(sourceClass));
		resolvers.add(getAnsiResolver());
		resolvers.add(getTitleResolver(sourceClass));

		registerPropertyResolver(resolvers, "git", this.gitPropertiesResource, "git.properties");
		registerPropertyResolver(resolvers, "build", this.buildInfoPropertiesResource, "build-info.properties");

		return resolvers;

	}

	private void registerPropertyResolver(List<PropertyResolver> resolvers, String name, Resource resource, String fileName) {
		if (resource != null) {
			try {
				Properties properties = PropertiesLoaderUtils.loadProperties(resource);
				resolvers.add(getPropertyResolver(properties, name));
			}
			catch (Exception e) {
				logger.debug(fileName + " not found in classpath");
			}
		}
	}

	private PropertyResolver getPropertyResolver(Properties properties, String name) {
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources
				.addLast(new PropertiesPropertySource(name, properties));

		return new PropertySourcesPropertyResolver(propertySources);

	}

	private PropertyResolver getVersionResolver(Class<?> sourceClass) {
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources
				.addLast(new MapPropertySource("version", getVersionsMap(sourceClass)));
		return new PropertySourcesPropertyResolver(propertySources);
	}

	private Map<String, Object> getVersionsMap(Class<?> sourceClass) {
		String appVersion = getApplicationVersion(sourceClass);
		String bootVersion = getBootVersion();
		Map<String, Object> versions = new HashMap<String, Object>();
		versions.put("application.version", getVersionString(appVersion, false));
		versions.put("spring-boot.version", getVersionString(bootVersion, false));
		versions.put("application.formatted-version", getVersionString(appVersion, true));
		versions.put("spring-boot.formatted-version",
				getVersionString(bootVersion, true));
		return versions;
	}

	protected String getApplicationVersion(Class<?> sourceClass) {
		Package sourcePackage = (sourceClass == null ? null : sourceClass.getPackage());
		return (sourcePackage == null ? null : sourcePackage.getImplementationVersion());
	}

	protected String getBootVersion() {
		return SpringBootVersion.getVersion();
	}

	private String getVersionString(String version, boolean format) {
		if (version == null) {
			return "";
		}
		return (format ? " (v" + version + ")" : version);
	}

	private PropertyResolver getAnsiResolver() {
		MutablePropertySources sources = new MutablePropertySources();
		sources.addFirst(new AnsiPropertySource("ansi", true));
		return new PropertySourcesPropertyResolver(sources);
	}

	private PropertyResolver getTitleResolver(Class<?> sourceClass) {
		MutablePropertySources sources = new MutablePropertySources();
		String applicationTitle = getApplicationTitle(sourceClass);
		Map<String, Object> titleMap = Collections.<String, Object>singletonMap(
				"application.title", (applicationTitle == null ? "" : applicationTitle));
		sources.addFirst(new MapPropertySource("title", titleMap));
		return new PropertySourcesPropertyResolver(sources);
	}

	protected String getApplicationTitle(Class<?> sourceClass) {
		Package sourcePackage = (sourceClass == null ? null : sourceClass.getPackage());
		return (sourcePackage == null ? null : sourcePackage.getImplementationTitle());
	}

}

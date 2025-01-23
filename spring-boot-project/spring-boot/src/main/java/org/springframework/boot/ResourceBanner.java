/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot;

import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.ansi.AnsiPropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.log.LogMessage;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

/**
 * Banner implementation that prints from a source text {@link Resource}.
 *
 * @author Phillip Webb
 * @author Vedran Pavic
 * @author Toshiaki Maki
 * @author Krzysztof Krason
 * @author Moritz Halbritter
 * @since 1.2.0
 */
public class ResourceBanner implements Banner {

	private static final Log logger = LogFactory.getLog(ResourceBanner.class);

	private final Resource resource;

	public ResourceBanner(Resource resource) {
		Assert.notNull(resource, "'resource' must not be null");
		Assert.isTrue(resource.exists(), "'resource' must exist");
		this.resource = resource;
	}

	@Override
	public void printBanner(Environment environment, Class<?> sourceClass, PrintStream out) {
		try {
			String banner = StreamUtils.copyToString(this.resource.getInputStream(),
					environment.getProperty("spring.banner.charset", Charset.class, StandardCharsets.UTF_8));
			for (PropertyResolver resolver : getPropertyResolvers(environment, sourceClass)) {
				banner = resolver.resolvePlaceholders(banner);
			}
			out.println(banner);
		}
		catch (Exception ex) {
			logger.warn(LogMessage.format("Banner not printable: %s (%s: '%s')", this.resource, ex.getClass(),
					ex.getMessage()), ex);
		}
	}

	/**
	 * Return a mutable list of the {@link PropertyResolver} instances that will be used
	 * to resolve placeholders.
	 * @param environment the environment
	 * @param sourceClass the source class
	 * @return a mutable list of property resolvers
	 */
	protected List<PropertyResolver> getPropertyResolvers(Environment environment, Class<?> sourceClass) {
		MutablePropertySources sources = new MutablePropertySources();
		if (environment instanceof ConfigurableEnvironment configurableEnvironment) {
			configurableEnvironment.getPropertySources().forEach(sources::addLast);
		}
		sources.addLast(getTitleSource(sourceClass));
		sources.addLast(getAnsiSource());
		sources.addLast(getVersionSource(sourceClass, environment));
		List<PropertyResolver> resolvers = new ArrayList<>();
		resolvers.add(new PropertySourcesPropertyResolver(sources));
		return resolvers;
	}

	private MapPropertySource getTitleSource(Class<?> sourceClass) {
		String applicationTitle = getApplicationTitle(sourceClass);
		Map<String, Object> titleMap = Collections.singletonMap("application.title",
				(applicationTitle != null) ? applicationTitle : "");
		return new MapPropertySource("title", titleMap);
	}

	/**
	 * Return the application title that should be used for the source class. By default
	 * will use {@link Package#getImplementationTitle()}.
	 * @param sourceClass the source class
	 * @return the application title
	 */
	protected String getApplicationTitle(Class<?> sourceClass) {
		Package sourcePackage = (sourceClass != null) ? sourceClass.getPackage() : null;
		return (sourcePackage != null) ? sourcePackage.getImplementationTitle() : null;
	}

	private AnsiPropertySource getAnsiSource() {
		return new AnsiPropertySource("ansi", true);
	}

	private MapPropertySource getVersionSource(Class<?> sourceClass, Environment environment) {
		return new MapPropertySource("version", getVersionsMap(sourceClass, environment));
	}

	private Map<String, Object> getVersionsMap(Class<?> sourceClass, Environment environment) {
		String appVersion = getApplicationVersion(sourceClass);
		if (appVersion == null) {
			appVersion = getApplicationVersion(environment);
		}
		String bootVersion = getBootVersion();
		Map<String, Object> versions = new HashMap<>();
		versions.put("application.version", getVersionString(appVersion, false));
		versions.put("spring-boot.version", getVersionString(bootVersion, false));
		versions.put("application.formatted-version", getVersionString(appVersion, true));
		versions.put("spring-boot.formatted-version", getVersionString(bootVersion, true));
		return versions;
	}

	/**
	 * Returns the application version.
	 * @param sourceClass the source class
	 * @return the application version or {@code null} if unknown
	 * @deprecated since 3.4.0 for removal in 3.6.0
	 */
	@Deprecated(since = "3.4.0", forRemoval = true)
	protected String getApplicationVersion(Class<?> sourceClass) {
		return null;
	}

	private String getApplicationVersion(Environment environment) {
		return environment.getProperty("spring.application.version");
	}

	protected String getBootVersion() {
		return SpringBootVersion.getVersion();
	}

	private String getVersionString(String version, boolean format) {
		if (version == null) {
			return "";
		}
		return format ? " (v" + version + ")" : version;
	}

}

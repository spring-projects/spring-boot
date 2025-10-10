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

package org.springframework.boot;

import java.io.InputStream;
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
import org.jspecify.annotations.Nullable;

import org.springframework.boot.ansi.AnsiPropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.log.LogMessage;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

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
	public void printBanner(Environment environment, @Nullable Class<?> sourceClass, PrintStream out) {
		try (InputStream input = this.resource.getInputStream()) {
			String banner = StreamUtils.copyToString(input,
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
	protected List<PropertyResolver> getPropertyResolvers(Environment environment, @Nullable Class<?> sourceClass) {
		List<PropertyResolver> resolvers = new ArrayList<>();
		resolvers.add(new PropertySourcesPropertyResolver(createNullDefaultSources(environment, sourceClass)));
		resolvers.add(new PropertySourcesPropertyResolver(createEmptyDefaultSources(environment, sourceClass)));
		return resolvers;
	}

	private MutablePropertySources createNullDefaultSources(Environment environment, @Nullable Class<?> sourceClass) {
		MutablePropertySources nullDefaultSources = new MutablePropertySources();
		if (environment instanceof ConfigurableEnvironment configurableEnvironment) {
			configurableEnvironment.getPropertySources().forEach(nullDefaultSources::addLast);
		}
		nullDefaultSources.addLast(getTitleSource(sourceClass, null));
		nullDefaultSources.addLast(getAnsiSource());
		nullDefaultSources.addLast(getVersionSource(environment, null));
		return nullDefaultSources;
	}

	private MutablePropertySources createEmptyDefaultSources(Environment environment, @Nullable Class<?> sourceClass) {
		MutablePropertySources emptyDefaultSources = new MutablePropertySources();
		emptyDefaultSources.addLast(getTitleSource(sourceClass, ""));
		emptyDefaultSources.addLast(getVersionSource(environment, ""));
		return emptyDefaultSources;
	}

	private MapWithNullsPropertySource getTitleSource(@Nullable Class<?> sourceClass, @Nullable String defaultValue) {
		String applicationTitle = getApplicationTitle(sourceClass);
		Map<String, @Nullable Object> titleMap = Collections.singletonMap("application.title",
				(applicationTitle != null) ? applicationTitle : defaultValue);
		return new MapWithNullsPropertySource("title", titleMap);
	}

	/**
	 * Return the application title that should be used for the source class. By default
	 * will use {@link Package#getImplementationTitle()}.
	 * @param sourceClass the source class
	 * @return the application title
	 */
	protected @Nullable String getApplicationTitle(@Nullable Class<?> sourceClass) {
		Package sourcePackage = (sourceClass != null) ? sourceClass.getPackage() : null;
		return (sourcePackage != null) ? sourcePackage.getImplementationTitle() : null;
	}

	private AnsiPropertySource getAnsiSource() {
		return new AnsiPropertySource("ansi", true);
	}

	private MapWithNullsPropertySource getVersionSource(Environment environment, @Nullable String defaultValue) {
		return new MapWithNullsPropertySource("version", getVersionsMap(environment, defaultValue));
	}

	private Map<String, @Nullable Object> getVersionsMap(Environment environment, @Nullable String defaultValue) {
		String appVersion = getApplicationVersion(environment);
		String bootVersion = getBootVersion();
		Map<String, @Nullable Object> versions = new HashMap<>();
		versions.put("application.version", getVersionString(appVersion, false, defaultValue));
		versions.put("spring-boot.version", getVersionString(bootVersion, false, defaultValue));
		versions.put("application.formatted-version", getVersionString(appVersion, true, defaultValue));
		versions.put("spring-boot.formatted-version", getVersionString(bootVersion, true, defaultValue));
		return versions;
	}

	private @Nullable String getApplicationVersion(Environment environment) {
		return environment.getProperty("spring.application.version");
	}

	protected @Nullable String getBootVersion() {
		return SpringBootVersion.getVersion();
	}

	private @Nullable String getVersionString(@Nullable String version, boolean format, @Nullable String fallback) {
		if (version == null) {
			return fallback;
		}
		return format ? " (v" + version + ")" : version;
	}

	/**
	 * Like {@link MapPropertySource}, but allows {@code null} as map values.
	 */
	private static class MapWithNullsPropertySource extends EnumerablePropertySource<Map<String, @Nullable Object>> {

		MapWithNullsPropertySource(String name, Map<String, @Nullable Object> source) {
			super(name, source);
		}

		@Override
		public String[] getPropertyNames() {
			return StringUtils.toStringArray(this.source.keySet());
		}

		@Override
		public @Nullable Object getProperty(String name) {
			return this.source.get(name);
		}

		@Override
		public boolean containsProperty(String name) {
			return this.source.containsKey(name);
		}

	}

}

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
 * @since 1.2.0
 */
public class ResourceBanner implements Banner {

	private static final Log logger = LogFactory.getLog(ResourceBanner.class);

	private final Resource resource;

	/**
     * Constructs a new ResourceBanner object with the given resource.
     * 
     * @param resource the resource to be used for the banner
     * @throws IllegalArgumentException if the resource is null
     * @throws IllegalArgumentException if the resource does not exist
     */
    public ResourceBanner(Resource resource) {
		Assert.notNull(resource, "Resource must not be null");
		Assert.isTrue(resource.exists(), "Resource must exist");
		this.resource = resource;
	}

	/**
     * Prints the banner to the specified output stream.
     *
     * @param environment the environment to resolve property placeholders
     * @param sourceClass the source class used for resolving property placeholders
     * @param out the output stream to print the banner to
     */
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
		if (environment instanceof ConfigurableEnvironment) {
			((ConfigurableEnvironment) environment).getPropertySources().forEach(sources::addLast);
		}
		sources.addLast(getTitleSource(sourceClass));
		sources.addLast(getAnsiSource());
		sources.addLast(getVersionSource(sourceClass));
		List<PropertyResolver> resolvers = new ArrayList<>();
		resolvers.add(new PropertySourcesPropertyResolver(sources));
		return resolvers;
	}

	/**
     * Returns a MapPropertySource containing the application title.
     * 
     * @param sourceClass the class used to retrieve the application title
     * @return a MapPropertySource containing the application title
     */
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

	/**
     * Returns an instance of AnsiPropertySource.
     * 
     * @return an instance of AnsiPropertySource
     */
    private AnsiPropertySource getAnsiSource() {
		return new AnsiPropertySource("ansi", true);
	}

	/**
     * Returns a MapPropertySource containing the versions map for the given source class.
     *
     * @param sourceClass the class for which the versions map is to be generated
     * @return a MapPropertySource containing the versions map
     */
    private MapPropertySource getVersionSource(Class<?> sourceClass) {
		return new MapPropertySource("version", getVersionsMap(sourceClass));
	}

	/**
     * Returns a map containing the versions of the application and Spring Boot.
     * 
     * @param sourceClass the class from which to retrieve the application version
     * @return a map containing the versions of the application and Spring Boot
     */
    private Map<String, Object> getVersionsMap(Class<?> sourceClass) {
		String appVersion = getApplicationVersion(sourceClass);
		String bootVersion = getBootVersion();
		Map<String, Object> versions = new HashMap<>();
		versions.put("application.version", getVersionString(appVersion, false));
		versions.put("spring-boot.version", getVersionString(bootVersion, false));
		versions.put("application.formatted-version", getVersionString(appVersion, true));
		versions.put("spring-boot.formatted-version", getVersionString(bootVersion, true));
		return versions;
	}

	/**
     * Returns the version of the application.
     * 
     * @param sourceClass the class from which the version is obtained
     * @return the version of the application, or null if not available
     */
    protected String getApplicationVersion(Class<?> sourceClass) {
		Package sourcePackage = (sourceClass != null) ? sourceClass.getPackage() : null;
		return (sourcePackage != null) ? sourcePackage.getImplementationVersion() : null;
	}

	/**
     * Returns the version of Spring Boot being used.
     *
     * @return the version of Spring Boot
     */
    protected String getBootVersion() {
		return SpringBootVersion.getVersion();
	}

	/**
     * Returns the version string with optional formatting.
     * 
     * @param version the version string to be formatted
     * @param format  true if the version string should be formatted, false otherwise
     * @return the formatted version string if format is true, otherwise the original version string
     */
    private String getVersionString(String version, boolean format) {
		if (version == null) {
			return "";
		}
		return format ? " (v" + version + ")" : version;
	}

}

/*
 * Copyright 2012-2014 the original author or authors.
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

import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

/**
 * Banner implementation that prints from a source {@link Resource}.
 *
 * @author Phillip Webb
 * @since 1.2.0
 */
public class ResourceBanner implements Banner {

	private static final Log log = LogFactory.getLog(ResourceBanner.class);

	private Resource resource;

	public ResourceBanner(Resource resource) {
		Assert.notNull(resource, "Resource must not be null");
		Assert.isTrue(resource.exists(), "Resource must exist");
		this.resource = resource;
	}

	@Override
	public void printBanner(Environment environment, Class<?> sourceClass, PrintStream out) {
		try {
			String banner = StreamUtils.copyToString(
					this.resource.getInputStream(),
					environment.getProperty("banner.charset", Charset.class,
							Charset.forName("UTF-8")));

			for (PropertyResolver resolver : getPropertyResolvers(environment,
					sourceClass)) {
				banner = resolver.resolvePlaceholders(banner);
			}
			out.println(banner);
		}
		catch (Exception ex) {
			log.warn("Banner not printable: " + this.resource + " (" + ex.getClass()
					+ ": '" + ex.getMessage() + "')", ex);
		}
	}

	protected List<PropertyResolver> getPropertyResolvers(Environment environment,
			Class<?> sourceClass) {
		List<PropertyResolver> resolvers = new ArrayList<PropertyResolver>();
		resolvers.add(environment);
		resolvers.add(getVersionResolver(sourceClass));
		return resolvers;
	}

	private PropertyResolver getVersionResolver(Class<?> sourceClass) {
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addLast(new MapPropertySource("version",
				getVersionsMap(sourceClass)));
		return new PropertySourcesPropertyResolver(propertySources);
	}

	private Map<String, Object> getVersionsMap(Class<?> sourceClass) {
		String appVersion = getApplicationVersion(sourceClass);
		String bootVersion = getBootVersion();
		Map<String, Object> versions = new HashMap<String, Object>();
		versions.put("application.version", getVersionString(appVersion, false));
		versions.put("spring-boot.version", getVersionString(bootVersion, false));
		versions.put("application.formatted-version", getVersionString(appVersion, true));
		versions.put("spring-boot.formatted-version", getVersionString(bootVersion, true));
		return versions;
	}

	protected String getApplicationVersion(Class<?> sourceClass) {
		Package sourcePackage = (sourceClass == null ? null : sourceClass.getPackage());
		return (sourcePackage == null ? null : sourcePackage.getImplementationVersion());
	}

	protected String getBootVersion() {
		return Banner.class.getPackage().getImplementationVersion();
	}

	private String getVersionString(String version, boolean format) {
		if (version == null) {
			return "";
		}
		return (format ? " (v" + version + ")" : version);
	}

}

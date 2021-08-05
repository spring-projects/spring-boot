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

package org.springframework.boot.devtools.env;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.devtools.system.DevToolsEnablementDeducer;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link EnvironmentPostProcessor} to add devtools properties from the user's home
 * directory.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author HaiTao Zhang
 * @author Madhura Bhave
 * @since 1.3.0
 */
public class DevToolsHomePropertiesPostProcessor implements EnvironmentPostProcessor {

	private static final String LEGACY_FILE_NAME = ".spring-boot-devtools.properties";

	private static final String[] FILE_NAMES = new String[] { "spring-boot-devtools.yml", "spring-boot-devtools.yaml",
			"spring-boot-devtools.properties" };

	private static final String CONFIG_PATH = "/.config/spring-boot/";

	private static final Set<PropertySourceLoader> PROPERTY_SOURCE_LOADERS;

	private final Properties systemProperties;

	private final Map<String, String> environmentVariables;

	static {
		Set<PropertySourceLoader> propertySourceLoaders = new HashSet<>();
		propertySourceLoaders.add(new PropertiesPropertySourceLoader());
		if (ClassUtils.isPresent("org.yaml.snakeyaml.Yaml", null)) {
			propertySourceLoaders.add(new YamlPropertySourceLoader());
		}
		PROPERTY_SOURCE_LOADERS = Collections.unmodifiableSet(propertySourceLoaders);
	}

	public DevToolsHomePropertiesPostProcessor() {
		this(System.getenv(), System.getProperties());
	}

	DevToolsHomePropertiesPostProcessor(Map<String, String> environmentVariables, Properties systemProperties) {
		this.environmentVariables = environmentVariables;
		this.systemProperties = systemProperties;
	}

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		if (DevToolsEnablementDeducer.shouldEnable(Thread.currentThread())) {
			List<PropertySource<?>> propertySources = getPropertySources();
			if (propertySources.isEmpty()) {
				addPropertySource(propertySources, LEGACY_FILE_NAME, (file) -> "devtools-local");
			}
			propertySources.forEach(environment.getPropertySources()::addFirst);
		}
	}

	private List<PropertySource<?>> getPropertySources() {
		List<PropertySource<?>> propertySources = new ArrayList<>();
		for (String fileName : FILE_NAMES) {
			addPropertySource(propertySources, CONFIG_PATH + fileName, this::getPropertySourceName);
		}
		return propertySources;
	}

	private String getPropertySourceName(File file) {
		return "devtools-local: [" + file.toURI() + "]";
	}

	private void addPropertySource(List<PropertySource<?>> propertySources, String fileName,
			Function<File, String> propertySourceNamer) {
		File home = getHomeDirectory();
		File file = (home != null) ? new File(home, fileName) : null;
		FileSystemResource resource = (file != null) ? new FileSystemResource(file) : null;
		if (resource != null && resource.exists() && resource.isFile()) {
			addPropertySource(propertySources, resource, propertySourceNamer);
		}
	}

	private void addPropertySource(List<PropertySource<?>> propertySources, FileSystemResource resource,
			Function<File, String> propertySourceNamer) {
		try {
			String name = propertySourceNamer.apply(resource.getFile());
			for (PropertySourceLoader loader : PROPERTY_SOURCE_LOADERS) {
				if (canLoadFileExtension(loader, resource.getFilename())) {
					propertySources.addAll(loader.load(name, resource));
				}
			}
		}
		catch (IOException ex) {
			throw new IllegalStateException("Unable to load " + resource.getFilename(), ex);
		}
	}

	private boolean canLoadFileExtension(PropertySourceLoader loader, String name) {
		return Arrays.stream(loader.getFileExtensions())
				.anyMatch((fileExtension) -> StringUtils.endsWithIgnoreCase(name, fileExtension));
	}

	protected File getHomeDirectory() {
		return getHomeDirectory(() -> this.environmentVariables.get("SPRING_DEVTOOLS_HOME"),
				() -> this.systemProperties.getProperty("spring.devtools.home"),
				() -> this.systemProperties.getProperty("user.home"));
	}

	@SafeVarargs
	private final File getHomeDirectory(Supplier<String>... pathSuppliers) {
		for (Supplier<String> pathSupplier : pathSuppliers) {
			String path = pathSupplier.get();
			if (StringUtils.hasText(path)) {
				return new File(path);
			}
		}
		return null;
	}

}

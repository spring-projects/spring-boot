/*
 * Copyright 2012-2019 the original author or authors.
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
import java.util.List;
import java.util.Properties;
import java.util.function.Function;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.devtools.DevToolsEnablementDeducer;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.StringUtils;

/**
 * {@link EnvironmentPostProcessor} to add devtools properties from the user's home
 * folder.
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

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		if (DevToolsEnablementDeducer.shouldEnable(Thread.currentThread())) {
			List<PropertySource> propertySources = getPropertySources();
			if (propertySources.isEmpty()) {
				addPropertySource(LEGACY_FILE_NAME, (file) -> "devtools-local", propertySources);
			}
			propertySources.forEach((source) -> environment.getPropertySources().addFirst(source));
		}
	}

	private List<PropertySource> getPropertySources() {
		List<PropertySource> propertySources = new ArrayList<>();
		for (String fileName : FILE_NAMES) {
			addPropertySource(CONFIG_PATH + fileName, (file) -> "devtools-local: [" + file.toURI() + "]",
					propertySources);
		}
		return propertySources;
	}

	private void addPropertySource(String fileName, Function<File, String> propertySourceName,
			List<PropertySource> propertySources) {
		Properties properties;
		File home = getHomeFolder();
		File propertyFile = (home != null) ? new File(home, fileName) : null;
		if (propertyFile != null && propertyFile.exists() && propertyFile.isFile()) {
			FileSystemResource resource = new FileSystemResource(propertyFile);
			try {
				properties = PropertiesLoaderUtils.loadProperties(resource);
				propertySources.add(new PropertiesPropertySource(propertySourceName.apply(propertyFile), properties));
			}
			catch (IOException ex) {
				throw new IllegalStateException("Unable to load " + fileName, ex);
			}
		}
	}

	protected File getHomeFolder() {
		String home = System.getProperty("user.home");
		if (StringUtils.hasLength(home)) {
			return new File(home);
		}
		return null;
	}

}

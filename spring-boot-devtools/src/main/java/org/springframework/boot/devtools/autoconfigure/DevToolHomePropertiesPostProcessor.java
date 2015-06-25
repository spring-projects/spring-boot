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

package org.springframework.boot.devtools.autoconfigure;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.StringUtils;

/**
 * {@link BeanFactoryPostProcessor} to add devtools properties from the users home folder.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
public class DevToolHomePropertiesPostProcessor implements BeanFactoryPostProcessor,
		EnvironmentAware {

	private static final String FILE_NAME = ".spring-boot-devtools.properties";

	private Environment environment;

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
			throws BeansException {
		if (this.environment instanceof ConfigurableEnvironment) {
			try {
				postProcessEnvironment((ConfigurableEnvironment) this.environment);
			}
			catch (IOException ex) {
				throw new IllegalStateException("Unable to load " + FILE_NAME, ex);
			}
		}
	}

	private void postProcessEnvironment(ConfigurableEnvironment environment)
			throws IOException {
		File home = getHomeFolder();
		File propertyFile = (home == null ? null : new File(home, FILE_NAME));
		if (propertyFile != null && propertyFile.exists() && propertyFile.isFile()) {
			FileSystemResource resource = new FileSystemResource(propertyFile);
			Properties properties = PropertiesLoaderUtils.loadProperties(resource);
			environment.getPropertySources().addFirst(
					new PropertiesPropertySource("devtools-local", properties));
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

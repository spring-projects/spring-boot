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

package org.springframework.boot.developertools.autoconfigure;

import java.net.URL;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.developertools.classpath.ClassPathChangedEvent;
import org.springframework.boot.developertools.classpath.ClassPathFileSystemWatcher;
import org.springframework.boot.developertools.classpath.ClassPathRestartStrategy;
import org.springframework.boot.developertools.classpath.PatternClassPathRestartStrategy;
import org.springframework.boot.developertools.restart.ConditionalOnInitializedRestarter;
import org.springframework.boot.developertools.restart.Restarter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for local development support.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
@Configuration
@ConditionalOnInitializedRestarter
@EnableConfigurationProperties(DeveloperToolsProperties.class)
public class LocalDeveloperToolsAutoConfiguration {

	@Autowired
	private DeveloperToolsProperties properties;

	@Bean
	public static LocalDeveloperPropertyDefaultsPostProcessor localDeveloperPropertyDefaultsPostProcessor() {
		return new LocalDeveloperPropertyDefaultsPostProcessor();
	}

	/**
	 * Local Restart Configuration.
	 */
	@ConditionalOnProperty(prefix = "spring.developertools.restart", name = "enabled", matchIfMissing = true)
	static class RestartConfiguration {

		@Autowired
		private DeveloperToolsProperties properties;

		@Bean
		@ConditionalOnMissingBean
		public ClassPathFileSystemWatcher classPathFileSystemWatcher() {
			URL[] urls = Restarter.getInstance().getInitialUrls();
			return new ClassPathFileSystemWatcher(classPathRestartStrategy(), urls);
		}

		@Bean
		@ConditionalOnMissingBean
		public ClassPathRestartStrategy classPathRestartStrategy() {
			return new PatternClassPathRestartStrategy(this.properties.getRestart()
					.getExclude());
		}

		@EventListener
		public void onClassPathChanged(ClassPathChangedEvent event) {
			if (event.isRestartRequired()) {
				Restarter.getInstance().restart();
			}
		}

	}

}

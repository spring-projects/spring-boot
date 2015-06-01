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

package org.springframework.boot.developertools.remote.client;

import java.net.URL;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.developertools.autoconfigure.DeveloperToolsProperties;
import org.springframework.boot.developertools.autoconfigure.RemoteDeveloperToolsProperties;
import org.springframework.boot.developertools.classpath.ClassPathFileSystemWatcher;
import org.springframework.boot.developertools.classpath.ClassPathRestartStrategy;
import org.springframework.boot.developertools.classpath.PatternClassPathRestartStrategy;
import org.springframework.boot.developertools.restart.DefaultRestartInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

/**
 * Configuration used to connect to remote Spring Boot applications.
 *
 * @author Phillip Webb
 * @since 1.3.0
 * @see org.springframework.boot.developertools.RemoteSpringApplication
 */
@Configuration
@EnableConfigurationProperties(DeveloperToolsProperties.class)
public class RemoteClientConfiguration {

	private static final Log logger = LogFactory.getLog(RemoteClientConfiguration.class);

	@Autowired
	private DeveloperToolsProperties properties;

	@Value("${remoteUrl}")
	private String remoteUrl;

	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	@Bean
	public ClientHttpRequestFactory clientHttpRequestFactory() {
		return new SimpleClientHttpRequestFactory();
	}

	@PostConstruct
	private void logWarnings() {
		RemoteDeveloperToolsProperties remoteProperties = this.properties.getRemote();
		if (!remoteProperties.getRestart().isEnabled()) {
			logger.warn("Remote restart is not enabled.");
		}
	}

	/**
	 * Client configuration for remote update and restarts.
	 */
	@ConditionalOnProperty(prefix = "spring.developertools.remote.restart", name = "enabled", matchIfMissing = true)
	static class RemoteRestartClientConfiguration {

		@Autowired
		private DeveloperToolsProperties properties;

		@Value("${remoteUrl}")
		private String remoteUrl;

		@Bean
		public ClassPathFileSystemWatcher classPathFileSystemWatcher() {
			DefaultRestartInitializer restartInitializer = new DefaultRestartInitializer();
			URL[] urls = restartInitializer.getInitialUrls(Thread.currentThread());
			if (urls == null) {
				urls = new URL[0];
			}
			return new ClassPathFileSystemWatcher(classPathRestartStrategy(), urls);
		}

		@Bean
		public ClassPathRestartStrategy classPathRestartStrategy() {
			return new PatternClassPathRestartStrategy(this.properties.getRestart()
					.getExclude());
		}

		@Bean
		public ClassPathChangeUploader classPathChangeUploader(
				ClientHttpRequestFactory requestFactory) {
			String url = this.remoteUrl + this.properties.getRemote().getContextPath()
					+ "/restart";
			return new ClassPathChangeUploader(url, requestFactory);
		}

	}

}

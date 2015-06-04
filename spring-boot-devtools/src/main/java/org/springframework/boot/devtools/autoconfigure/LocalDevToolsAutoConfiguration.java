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

import java.net.URL;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.devtools.classpath.ClassPathChangedEvent;
import org.springframework.boot.devtools.classpath.ClassPathFileSystemWatcher;
import org.springframework.boot.devtools.classpath.ClassPathRestartStrategy;
import org.springframework.boot.devtools.classpath.PatternClassPathRestartStrategy;
import org.springframework.boot.devtools.filewatch.FileSystemWatcher;
import org.springframework.boot.devtools.livereload.LiveReloadServer;
import org.springframework.boot.devtools.restart.ConditionalOnInitializedRestarter;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.devtools.restart.Restarter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for local development support.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
@Configuration
@ConditionalOnInitializedRestarter
@EnableConfigurationProperties(DevToolsProperties.class)
public class LocalDevToolsAutoConfiguration {

	@Autowired
	private DevToolsProperties properties;

	@Bean
	public static DevToolsPropertyDefaultsPostProcessor devToolsPropertyDefaultsPostProcessor() {
		return new DevToolsPropertyDefaultsPostProcessor();
	}

	/**
	 * Local LiveReload configuration.
	 */
	@ConditionalOnProperty(prefix = "spring.devtools.livereload", name = "enabled", matchIfMissing = true)
	static class LiveReloadConfiguration {

		@Autowired
		private DevToolsProperties properties;

		@Autowired(required = false)
		private LiveReloadServer liveReloadServer;

		@Bean
		@RestartScope
		@ConditionalOnMissingBean
		public LiveReloadServer liveReloadServer() {
			return new LiveReloadServer(this.properties.getLivereload().getPort(),
					Restarter.getInstance().getThreadFactory());
		}

		@EventListener
		public void onContextRefreshed(ContextRefreshedEvent event) {
			optionalLiveReloadServer().triggerReload();
		}

		@EventListener
		public void onClassPathChanged(ClassPathChangedEvent event) {
			if (!event.isRestartRequired()) {
				optionalLiveReloadServer().triggerReload();
			}
		}

		@Bean
		public OptionalLiveReloadServer optionalLiveReloadServer() {
			return new OptionalLiveReloadServer(this.liveReloadServer);
		}

	}

	/**
	 * Local Restart Configuration.
	 */
	@ConditionalOnProperty(prefix = "spring.devtools.restart", name = "enabled", matchIfMissing = true)
	static class RestartConfiguration {

		@Autowired
		private DevToolsProperties properties;

		private final FileSystemWatcher fileSystemWatcher = new FileSystemWatcher();

		@Bean
		@ConditionalOnMissingBean
		public ClassPathFileSystemWatcher classPathFileSystemWatcher() {
			URL[] urls = Restarter.getInstance().getInitialUrls();
			return new ClassPathFileSystemWatcher(this.fileSystemWatcher,
					classPathRestartStrategy(), urls);
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
				this.fileSystemWatcher.stop();
				Restarter.getInstance().restart();
			}
		}

	}

}

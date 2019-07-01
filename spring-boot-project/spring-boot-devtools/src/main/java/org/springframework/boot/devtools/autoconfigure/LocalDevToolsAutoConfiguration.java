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

package org.springframework.boot.devtools.autoconfigure;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.devtools.autoconfigure.DevToolsProperties.Restart;
import org.springframework.boot.devtools.classpath.ClassPathChangedEvent;
import org.springframework.boot.devtools.classpath.ClassPathFileSystemWatcher;
import org.springframework.boot.devtools.classpath.ClassPathRestartStrategy;
import org.springframework.boot.devtools.classpath.PatternClassPathRestartStrategy;
import org.springframework.boot.devtools.filewatch.FileSystemWatcher;
import org.springframework.boot.devtools.filewatch.FileSystemWatcherFactory;
import org.springframework.boot.devtools.livereload.LiveReloadServer;
import org.springframework.boot.devtools.restart.ConditionalOnInitializedRestarter;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.devtools.restart.Restarter;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.ResolvableType;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for local development support.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Vladimir Tsanev
 * @since 1.3.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnInitializedRestarter
@EnableConfigurationProperties(DevToolsProperties.class)
public class LocalDevToolsAutoConfiguration {

	/**
	 * Local LiveReload configuration.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(prefix = "spring.devtools.livereload", name = "enabled", matchIfMissing = true)
	static class LiveReloadConfiguration {

		@Bean
		@RestartScope
		@ConditionalOnMissingBean
		LiveReloadServer liveReloadServer(DevToolsProperties properties) {
			return new LiveReloadServer(properties.getLivereload().getPort(),
					Restarter.getInstance().getThreadFactory());
		}

		@Bean
		OptionalLiveReloadServer optionalLiveReloadServer(LiveReloadServer liveReloadServer) {
			return new OptionalLiveReloadServer(liveReloadServer);
		}

		@Bean
		LiveReloadServerEventListener liveReloadServerEventListener(OptionalLiveReloadServer liveReloadServer) {
			return new LiveReloadServerEventListener(liveReloadServer);
		}

	}

	/**
	 * Local Restart Configuration.
	 */
	@Lazy(false)
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(prefix = "spring.devtools.restart", name = "enabled", matchIfMissing = true)
	static class RestartConfiguration {

		private final DevToolsProperties properties;

		RestartConfiguration(DevToolsProperties properties) {
			this.properties = properties;
		}

		@Bean
		ApplicationListener<ClassPathChangedEvent> restartingClassPathChangedEventListener(
				FileSystemWatcherFactory fileSystemWatcherFactory) {
			return (event) -> {
				if (event.isRestartRequired()) {
					Restarter.getInstance().restart(new FileWatchingFailureHandler(fileSystemWatcherFactory));
				}
			};
		}

		@Bean
		@ConditionalOnMissingBean
		ClassPathFileSystemWatcher classPathFileSystemWatcher(FileSystemWatcherFactory fileSystemWatcherFactory,
				ClassPathRestartStrategy classPathRestartStrategy) {
			URL[] urls = Restarter.getInstance().getInitialUrls();
			ClassPathFileSystemWatcher watcher = new ClassPathFileSystemWatcher(fileSystemWatcherFactory,
					classPathRestartStrategy, urls);
			watcher.setStopWatcherOnRestart(true);
			return watcher;
		}

		@Bean
		@ConditionalOnMissingBean
		ClassPathRestartStrategy classPathRestartStrategy() {
			return new PatternClassPathRestartStrategy(this.properties.getRestart().getAllExclude());
		}

		@Bean
		FileSystemWatcherFactory fileSystemWatcherFactory() {
			return this::newFileSystemWatcher;
		}

		@Bean
		@ConditionalOnProperty(prefix = "spring.devtools.restart", name = "log-condition-evaluation-delta",
				matchIfMissing = true)
		ConditionEvaluationDeltaLoggingListener conditionEvaluationDeltaLoggingListener() {
			return new ConditionEvaluationDeltaLoggingListener();
		}

		private FileSystemWatcher newFileSystemWatcher() {
			Restart restartProperties = this.properties.getRestart();
			FileSystemWatcher watcher = new FileSystemWatcher(true, restartProperties.getPollInterval(),
					restartProperties.getQuietPeriod());
			String triggerFile = restartProperties.getTriggerFile();
			if (StringUtils.hasLength(triggerFile)) {
				watcher.setTriggerFilter(new TriggerFileFilter(triggerFile));
			}
			List<File> additionalPaths = restartProperties.getAdditionalPaths();
			for (File path : additionalPaths) {
				watcher.addSourceFolder(path.getAbsoluteFile());
			}
			return watcher;
		}

	}

	static class LiveReloadServerEventListener implements GenericApplicationListener {

		private final OptionalLiveReloadServer liveReloadServer;

		LiveReloadServerEventListener(OptionalLiveReloadServer liveReloadServer) {
			this.liveReloadServer = liveReloadServer;
		}

		@Override
		public boolean supportsEventType(ResolvableType eventType) {
			Class<?> type = eventType.getRawClass();
			if (type == null) {
				return false;
			}
			return ContextRefreshedEvent.class.isAssignableFrom(type)
					|| ClassPathChangedEvent.class.isAssignableFrom(type);
		}

		@Override
		public boolean supportsSourceType(Class<?> sourceType) {
			return true;
		}

		@Override
		public void onApplicationEvent(ApplicationEvent event) {
			if (event instanceof ContextRefreshedEvent || (event instanceof ClassPathChangedEvent
					&& !((ClassPathChangedEvent) event).isRestartRequired())) {
				this.liveReloadServer.triggerReload();
			}
		}

		@Override
		public int getOrder() {
			return 0;
		}

	}

}

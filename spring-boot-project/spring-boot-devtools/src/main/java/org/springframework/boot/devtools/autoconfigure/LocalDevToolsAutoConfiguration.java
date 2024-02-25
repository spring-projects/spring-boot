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

package org.springframework.boot.devtools.autoconfigure;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.autoconfigure.AutoConfiguration;
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
import org.springframework.boot.devtools.filewatch.SnapshotStateRepository;
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
import org.springframework.core.log.LogMessage;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for local development support.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Vladimir Tsanev
 * @since 1.3.0
 */
@AutoConfiguration
@ConditionalOnInitializedRestarter
@EnableConfigurationProperties(DevToolsProperties.class)
public class LocalDevToolsAutoConfiguration {

	/**
	 * Local LiveReload configuration.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(prefix = "spring.devtools.livereload", name = "enabled", matchIfMissing = true)
	static class LiveReloadConfiguration {

		/**
         * Creates a new instance of LiveReloadServer with the specified port and thread factory.
         * 
         * @param properties the DevToolsProperties object containing the livereload configuration
         * @return a new instance of LiveReloadServer
         */
        @Bean
		@RestartScope
		@ConditionalOnMissingBean
		LiveReloadServer liveReloadServer(DevToolsProperties properties) {
			return new LiveReloadServer(properties.getLivereload().getPort(),
					Restarter.getInstance().getThreadFactory());
		}

		/**
         * Creates an OptionalLiveReloadServer object with the provided LiveReloadServer.
         * 
         * @param liveReloadServer the LiveReloadServer to be wrapped in an OptionalLiveReloadServer
         * @return an OptionalLiveReloadServer object containing the provided LiveReloadServer
         */
        @Bean
		OptionalLiveReloadServer optionalLiveReloadServer(LiveReloadServer liveReloadServer) {
			return new OptionalLiveReloadServer(liveReloadServer);
		}

		/**
         * Creates a new instance of LiveReloadServerEventListener with the provided optional LiveReloadServer.
         * 
         * @param liveReloadServer the optional LiveReloadServer to be used by the event listener
         * @return a new instance of LiveReloadServerEventListener
         */
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

		/**
         * Initializes a new instance of the RestartConfiguration class with the specified DevToolsProperties.
         * 
         * @param properties The DevToolsProperties to be used for the restart configuration.
         */
        RestartConfiguration(DevToolsProperties properties) {
			this.properties = properties;
		}

		/**
         * Creates a new instance of the RestartingClassPathChangeChangedEventListener class.
         * 
         * @param fileSystemWatcherFactory the factory used to create a file system watcher
         * @return the created RestartingClassPathChangeChangedEventListener instance
         */
        @Bean
		RestartingClassPathChangeChangedEventListener restartingClassPathChangedEventListener(
				FileSystemWatcherFactory fileSystemWatcherFactory) {
			return new RestartingClassPathChangeChangedEventListener(fileSystemWatcherFactory);
		}

		/**
         * Creates a ClassPathFileSystemWatcher bean if no other bean of the same type is present.
         * 
         * @param fileSystemWatcherFactory The factory used to create the FileSystemWatcher.
         * @param classPathRestartStrategy The strategy used for restarting the classpath.
         * @return The created ClassPathFileSystemWatcher bean.
         */
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

		/**
         * Creates a new instance of ClassPathRestartStrategy if no other bean of this type is present in the application context.
         * This strategy is used for restarting the application by monitoring changes in the classpath.
         * The strategy excludes all the classes specified in the restart configuration properties.
         *
         * @return the ClassPathRestartStrategy instance
         */
        @Bean
		@ConditionalOnMissingBean
		ClassPathRestartStrategy classPathRestartStrategy() {
			return new PatternClassPathRestartStrategy(this.properties.getRestart().getAllExclude());
		}

		/**
         * Creates a factory for FileSystemWatcher objects.
         *
         * @return the factory for FileSystemWatcher objects
         */
        @Bean
		FileSystemWatcherFactory fileSystemWatcherFactory() {
			return this::newFileSystemWatcher;
		}

		/**
         * Creates a new instance of {@link ConditionEvaluationDeltaLoggingListener} if the property
         * "spring.devtools.restart.log-condition-evaluation-delta" is present in the application's
         * configuration. If the property is missing, the method will still be executed due to the
         * "matchIfMissing" attribute being set to true.
         *
         * @return a new instance of {@link ConditionEvaluationDeltaLoggingListener} if the property
         * "spring.devtools.restart.log-condition-evaluation-delta" is present, otherwise null.
         */
        @Bean
		@ConditionalOnProperty(prefix = "spring.devtools.restart", name = "log-condition-evaluation-delta",
				matchIfMissing = true)
		ConditionEvaluationDeltaLoggingListener conditionEvaluationDeltaLoggingListener() {
			return new ConditionEvaluationDeltaLoggingListener();
		}

		/**
         * Creates a new instance of FileSystemWatcher with the specified restart properties.
         * 
         * @return a new instance of FileSystemWatcher
         */
        private FileSystemWatcher newFileSystemWatcher() {
			Restart restartProperties = this.properties.getRestart();
			FileSystemWatcher watcher = new FileSystemWatcher(true, restartProperties.getPollInterval(),
					restartProperties.getQuietPeriod(), SnapshotStateRepository.STATIC);
			String triggerFile = restartProperties.getTriggerFile();
			if (StringUtils.hasLength(triggerFile)) {
				watcher.setTriggerFilter(new TriggerFileFilter(triggerFile));
			}
			List<File> additionalPaths = restartProperties.getAdditionalPaths();
			for (File path : additionalPaths) {
				watcher.addSourceDirectory(path.getAbsoluteFile());
			}
			return watcher;
		}

	}

	/**
     * LiveReloadServerEventListener class.
     */
    static class LiveReloadServerEventListener implements GenericApplicationListener {

		private final OptionalLiveReloadServer liveReloadServer;

		/**
         * Constructs a new LiveReloadServerEventListener with an optional LiveReloadServer.
         * 
         * @param liveReloadServer the optional LiveReloadServer to be associated with the event listener
         */
        LiveReloadServerEventListener(OptionalLiveReloadServer liveReloadServer) {
			this.liveReloadServer = liveReloadServer;
		}

		/**
         * Determines whether the specified event type is supported by this listener.
         * 
         * @param eventType the event type to check
         * @return true if the event type is supported, false otherwise
         */
        @Override
		public boolean supportsEventType(ResolvableType eventType) {
			Class<?> type = eventType.getRawClass();
			if (type == null) {
				return false;
			}
			return ContextRefreshedEvent.class.isAssignableFrom(type)
					|| ClassPathChangedEvent.class.isAssignableFrom(type);
		}

		/**
         * Returns true if the specified source type is supported by this listener.
         * 
         * @param sourceType the source type to check
         * @return true if the source type is supported, false otherwise
         */
        @Override
		public boolean supportsSourceType(Class<?> sourceType) {
			return true;
		}

		/**
         * This method is called when an application event is triggered.
         * It checks if the event is either a ContextRefreshedEvent or a ClassPathChangedEvent
         * that does not require a restart. If so, it triggers a reload of the LiveReload server.
         * 
         * @param event The application event that was triggered
         */
        @Override
		public void onApplicationEvent(ApplicationEvent event) {
			if (event instanceof ContextRefreshedEvent || (event instanceof ClassPathChangedEvent classPathChangedEvent
					&& !classPathChangedEvent.isRestartRequired())) {
				this.liveReloadServer.triggerReload();
			}
		}

		/**
         * Returns the order in which this event listener should be executed.
         * 
         * @return the order of execution for this event listener
         */
        @Override
		public int getOrder() {
			return 0;
		}

	}

	/**
     * RestartingClassPathChangeChangedEventListener class.
     */
    static class RestartingClassPathChangeChangedEventListener implements ApplicationListener<ClassPathChangedEvent> {

		private static final Log logger = LogFactory.getLog(RestartingClassPathChangeChangedEventListener.class);

		private final FileSystemWatcherFactory fileSystemWatcherFactory;

		/**
         * Constructs a new RestartingClassPathChangeChangedEventListener with the specified FileSystemWatcherFactory.
         * 
         * @param fileSystemWatcherFactory the factory used to create FileSystemWatcher instances
         */
        RestartingClassPathChangeChangedEventListener(FileSystemWatcherFactory fileSystemWatcherFactory) {
			this.fileSystemWatcherFactory = fileSystemWatcherFactory;
		}

		/**
         * This method is called when a ClassPathChangedEvent is triggered.
         * If a restart is required, it logs the reason for the restart and the change set.
         * It then restarts the application using the Restarter class.
         * 
         * @param event The ClassPathChangedEvent that triggered this method.
         */
        @Override
		public void onApplicationEvent(ClassPathChangedEvent event) {
			if (event.isRestartRequired()) {
				logger.info(LogMessage.format("Restarting due to %s", event.overview()));
				logger.debug(LogMessage.format("Change set: %s", event.getChangeSet()));
				Restarter.getInstance().restart(new FileWatchingFailureHandler(this.fileSystemWatcherFactory));
			}
		}

	}

}

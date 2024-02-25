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

package org.springframework.boot.devtools.remote.client;

import java.net.InetSocketAddress;
import java.net.Proxy.Type;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.devtools.autoconfigure.DevToolsProperties;
import org.springframework.boot.devtools.autoconfigure.DevToolsProperties.Restart;
import org.springframework.boot.devtools.autoconfigure.OptionalLiveReloadServer;
import org.springframework.boot.devtools.autoconfigure.RemoteDevToolsProperties;
import org.springframework.boot.devtools.autoconfigure.RemoteDevToolsProperties.Proxy;
import org.springframework.boot.devtools.autoconfigure.TriggerFileFilter;
import org.springframework.boot.devtools.classpath.ClassPathChangedEvent;
import org.springframework.boot.devtools.classpath.ClassPathFileSystemWatcher;
import org.springframework.boot.devtools.classpath.ClassPathRestartStrategy;
import org.springframework.boot.devtools.classpath.PatternClassPathRestartStrategy;
import org.springframework.boot.devtools.filewatch.FileSystemWatcher;
import org.springframework.boot.devtools.filewatch.FileSystemWatcherFactory;
import org.springframework.boot.devtools.livereload.LiveReloadServer;
import org.springframework.boot.devtools.restart.DefaultRestartInitializer;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.devtools.restart.Restarter;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.log.LogMessage;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Configuration used to connect to remote Spring Boot applications.
 *
 * @author Phillip Webb
 * @since 1.3.0
 * @see org.springframework.boot.devtools.RemoteSpringApplication
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DevToolsProperties.class)
public class RemoteClientConfiguration implements InitializingBean {

	private static final Log logger = LogFactory.getLog(RemoteClientConfiguration.class);

	private final DevToolsProperties properties;

	@Value("${remoteUrl}")
	private String remoteUrl;

	/**
	 * Constructs a new RemoteClientConfiguration object with the specified
	 * DevToolsProperties.
	 * @param properties the DevToolsProperties to be used for configuring the remote
	 * client
	 */
	public RemoteClientConfiguration(DevToolsProperties properties) {
		this.properties = properties;
	}

	/**
	 * Returns a new instance of PropertySourcesPlaceholderConfigurer.
	 * @return a new instance of PropertySourcesPlaceholderConfigurer
	 */
	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	/**
	 * Returns the ClientHttpRequestFactory bean used for creating HTTP requests to remote
	 * servers.
	 *
	 * This method creates a SimpleClientHttpRequestFactory and sets a proxy if configured
	 * in the application properties. It also adds a security interceptor to the list of
	 * interceptors.
	 * @return the ClientHttpRequestFactory bean
	 */
	@Bean
	public ClientHttpRequestFactory clientHttpRequestFactory() {
		List<ClientHttpRequestInterceptor> interceptors = Collections.singletonList(getSecurityInterceptor());
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		Proxy proxy = this.properties.getRemote().getProxy();
		if (proxy.getHost() != null && proxy.getPort() != null) {
			requestFactory
				.setProxy(new java.net.Proxy(Type.HTTP, new InetSocketAddress(proxy.getHost(), proxy.getPort())));
		}
		return new InterceptingClientHttpRequestFactory(requestFactory, interceptors);
	}

	/**
	 * Returns a security interceptor for the remote client. The interceptor adds a secret
	 * header to the HTTP request for authentication. The secret header name and value are
	 * retrieved from the remote properties.
	 * @return the security interceptor
	 * @throws IllegalStateException if the secret value is null
	 * @see RemoteDevToolsProperties#getSecretHeaderName()
	 * @see RemoteDevToolsProperties#getSecret()
	 * @see HttpHeaderInterceptor
	 */
	private ClientHttpRequestInterceptor getSecurityInterceptor() {
		RemoteDevToolsProperties remoteProperties = this.properties.getRemote();
		String secretHeaderName = remoteProperties.getSecretHeaderName();
		String secret = remoteProperties.getSecret();
		Assert.state(secret != null,
				"The environment value 'spring.devtools.remote.secret' is required to secure your connection.");
		return new HttpHeaderInterceptor(secretHeaderName, secret);
	}

	/**
	 * This method is called after all the properties have been set for the
	 * RemoteClientConfiguration class. It logs any warnings that may have occurred during
	 * the initialization process.
	 */
	@Override
	public void afterPropertiesSet() {
		logWarnings();
	}

	/**
	 * Logs any warnings related to the remote client configuration.
	 *
	 * This method checks if remote restart is disabled and logs a warning if it is. It
	 * also checks if the connection URL is insecure and logs a warning if it is not using
	 * HTTPS.
	 */
	private void logWarnings() {
		RemoteDevToolsProperties remoteProperties = this.properties.getRemote();
		if (!remoteProperties.getRestart().isEnabled()) {
			logger.warn("Remote restart is disabled.");
		}
		if (!this.remoteUrl.startsWith("https://")) {
			logger.warn(LogMessage.format(
					"The connection to %s is insecure. You should use a URL starting with 'https://'.",
					this.remoteUrl));
		}
	}

	/**
	 * LiveReload configuration.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(prefix = "spring.devtools.livereload", name = "enabled", matchIfMissing = true)
	static class LiveReloadConfiguration {

		private final DevToolsProperties properties;

		private final ClientHttpRequestFactory clientHttpRequestFactory;

		private final String remoteUrl;

		private final ExecutorService executor = Executors.newSingleThreadExecutor();

		/**
		 * Constructs a new LiveReloadConfiguration object with the specified properties,
		 * clientHttpRequestFactory, and remoteUrl.
		 * @param properties the DevToolsProperties object containing the configuration
		 * properties for the LiveReload server
		 * @param clientHttpRequestFactory the ClientHttpRequestFactory object used for
		 * making HTTP requests
		 * @param remoteUrl the URL of the remote server to connect to for LiveReload
		 * functionality
		 */
		LiveReloadConfiguration(DevToolsProperties properties, ClientHttpRequestFactory clientHttpRequestFactory,
				@Value("${remoteUrl}") String remoteUrl) {
			this.properties = properties;
			this.clientHttpRequestFactory = clientHttpRequestFactory;
			this.remoteUrl = remoteUrl;
		}

		/**
		 * Creates a new instance of LiveReloadServer.
		 * @return the LiveReloadServer instance
		 */
		@Bean
		@RestartScope
		@ConditionalOnMissingBean
		LiveReloadServer liveReloadServer() {
			return new LiveReloadServer(this.properties.getLivereload().getPort(),
					Restarter.getInstance().getThreadFactory());
		}

		/**
		 * Creates an ApplicationListener that listens for ClassPathChangedEvent and
		 * triggers a live reload if a change is detected.
		 * @param optionalLiveReloadServer an optional LiveReloadServer instance
		 * @return the ApplicationListener that triggers the live reload
		 */
		@Bean
		ApplicationListener<ClassPathChangedEvent> liveReloadTriggeringClassPathChangedEventListener(
				OptionalLiveReloadServer optionalLiveReloadServer) {
			return (event) -> {
				String url = this.remoteUrl + this.properties.getRemote().getContextPath();
				this.executor.execute(
						new DelayedLiveReloadTrigger(optionalLiveReloadServer, this.clientHttpRequestFactory, url));
			};
		}

		/**
		 * Creates an OptionalLiveReloadServer object with the provided LiveReloadServer
		 * object.
		 * @param liveReloadServer the ObjectProvider for LiveReloadServer
		 * @return an OptionalLiveReloadServer object with the provided LiveReloadServer
		 * object, or an empty Optional if LiveReloadServer is not available
		 */
		@Bean
		OptionalLiveReloadServer optionalLiveReloadServer(ObjectProvider<LiveReloadServer> liveReloadServer) {
			return new OptionalLiveReloadServer(liveReloadServer.getIfAvailable());
		}

		/**
		 * Returns the ExecutorService used by the LiveReloadConfiguration.
		 * @return the ExecutorService used by the LiveReloadConfiguration
		 */
		final ExecutorService getExecutor() {
			return this.executor;
		}

	}

	/**
	 * Client configuration for remote update and restarts.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(prefix = "spring.devtools.remote.restart", name = "enabled", matchIfMissing = true)
	static class RemoteRestartClientConfiguration {

		private final DevToolsProperties properties;

		/**
		 * Initializes a new instance of the RemoteRestartClientConfiguration class.
		 * @param properties the DevToolsProperties object containing the configuration
		 * properties for the remote restart client
		 */
		RemoteRestartClientConfiguration(DevToolsProperties properties) {
			this.properties = properties;
		}

		/**
		 * Creates a ClassPathFileSystemWatcher object with the given dependencies.
		 * @param fileSystemWatcherFactory The factory for creating FileSystemWatcher
		 * objects.
		 * @param classPathRestartStrategy The strategy for restarting the classpath.
		 * @return The created ClassPathFileSystemWatcher object.
		 */
		@Bean
		ClassPathFileSystemWatcher classPathFileSystemWatcher(FileSystemWatcherFactory fileSystemWatcherFactory,
				ClassPathRestartStrategy classPathRestartStrategy) {
			DefaultRestartInitializer restartInitializer = new DefaultRestartInitializer();
			URL[] urls = restartInitializer.getInitialUrls(Thread.currentThread());
			if (urls == null) {
				urls = new URL[0];
			}
			return new ClassPathFileSystemWatcher(fileSystemWatcherFactory, classPathRestartStrategy, urls);
		}

		/**
		 * Returns a factory for creating a FileSystemWatcher.
		 * @return the FileSystemWatcherFactory
		 */
		@Bean
		FileSystemWatcherFactory getFileSystemWatcherFactory() {
			return this::newFileSystemWatcher;
		}

		/**
		 * Creates a new instance of FileSystemWatcher with the specified restart
		 * properties.
		 * @return a new instance of FileSystemWatcher
		 */
		private FileSystemWatcher newFileSystemWatcher() {
			Restart restartProperties = this.properties.getRestart();
			FileSystemWatcher watcher = new FileSystemWatcher(true, restartProperties.getPollInterval(),
					restartProperties.getQuietPeriod());
			String triggerFile = restartProperties.getTriggerFile();
			if (StringUtils.hasLength(triggerFile)) {
				watcher.setTriggerFilter(new TriggerFileFilter(triggerFile));
			}
			return watcher;
		}

		/**
		 * Returns a new instance of ClassPathRestartStrategy.
		 * @return a new instance of ClassPathRestartStrategy
		 */
		@Bean
		ClassPathRestartStrategy classPathRestartStrategy() {
			return new PatternClassPathRestartStrategy(this.properties.getRestart().getAllExclude());
		}

		/**
		 * Creates a ClassPathChangeUploader bean with the provided
		 * ClientHttpRequestFactory and remoteUrl. The remoteUrl is obtained from the
		 * application properties and is used to construct the URL for restarting the
		 * remote server.
		 * @param requestFactory the ClientHttpRequestFactory used for creating HTTP
		 * requests
		 * @param remoteUrl the URL of the remote server obtained from the application
		 * properties
		 * @return a ClassPathChangeUploader bean configured with the remote URL and
		 * request factory
		 */
		@Bean
		ClassPathChangeUploader classPathChangeUploader(ClientHttpRequestFactory requestFactory,
				@Value("${remoteUrl}") String remoteUrl) {
			String url = remoteUrl + this.properties.getRemote().getContextPath() + "/restart";
			return new ClassPathChangeUploader(url, requestFactory);
		}

	}

}

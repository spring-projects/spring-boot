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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.servlet.Filter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.developertools.autoconfigure.DeveloperToolsProperties;
import org.springframework.boot.developertools.autoconfigure.OptionalLiveReloadServer;
import org.springframework.boot.developertools.autoconfigure.RemoteDeveloperToolsProperties;
import org.springframework.boot.developertools.classpath.ClassPathChangedEvent;
import org.springframework.boot.developertools.classpath.ClassPathFileSystemWatcher;
import org.springframework.boot.developertools.classpath.ClassPathRestartStrategy;
import org.springframework.boot.developertools.classpath.PatternClassPathRestartStrategy;
import org.springframework.boot.developertools.livereload.LiveReloadServer;
import org.springframework.boot.developertools.restart.DefaultRestartInitializer;
import org.springframework.boot.developertools.restart.RestartScope;
import org.springframework.boot.developertools.restart.Restarter;
import org.springframework.boot.developertools.tunnel.client.HttpTunnelConnection;
import org.springframework.boot.developertools.tunnel.client.TunnelClient;
import org.springframework.boot.developertools.tunnel.client.TunnelConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
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
		if (!remoteProperties.getDebug().isEnabled()
				&& !remoteProperties.getRestart().isEnabled()) {
			logger.warn("Remote restart and debug are both disabled.");
		}
		if (!this.remoteUrl.startsWith("https://")) {
			logger.warn("The connection to " + this.remoteUrl
					+ " is insecure. You should use a URL starting with 'https://'.");
		}
	}

	/**
	 * LiveReload configuration.
	 */
	@ConditionalOnProperty(prefix = "spring.developertools.livereload", name = "enabled", matchIfMissing = true)
	static class LiveReloadConfiguration {

		@Autowired
		private DeveloperToolsProperties properties;

		@Autowired(required = false)
		private LiveReloadServer liveReloadServer;

		@Autowired
		private ClientHttpRequestFactory clientHttpRequestFactory;

		@Value("${remoteUrl}")
		private String remoteUrl;

		private ExecutorService executor = Executors.newSingleThreadExecutor();

		@Bean
		@RestartScope
		@ConditionalOnMissingBean
		public LiveReloadServer liveReloadServer() {
			return new LiveReloadServer(this.properties.getLivereload().getPort(),
					Restarter.getInstance().getThreadFactory());
		}

		@EventListener
		public void onClassPathChanged(ClassPathChangedEvent event) {
			String url = this.remoteUrl + this.properties.getRemote().getContextPath();
			this.executor.execute(new DelayedLiveReloadTrigger(
					optionalLiveReloadServer(), this.clientHttpRequestFactory, url));
		}

		@Bean
		public OptionalLiveReloadServer optionalLiveReloadServer() {
			return new OptionalLiveReloadServer(this.liveReloadServer);
		}

		final ExecutorService getExecutor() {
			return this.executor;
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

	/**
	 * Client configuration for remote debug HTTP tunneling.
	 */
	@ConditionalOnProperty(prefix = "spring.developertools.remote.debug", name = "enabled", matchIfMissing = true)
	@ConditionalOnClass(Filter.class)
	@Conditional(LocalDebugPortAvailableCondition.class)
	static class RemoteDebugTunnelClientConfiguration {

		@Autowired
		private DeveloperToolsProperties properties;

		@Value("${remoteUrl}")
		private String remoteUrl;

		@Bean
		public TunnelClient remoteDebugTunnelClient(
				ClientHttpRequestFactory requestFactory) {
			RemoteDeveloperToolsProperties remoteProperties = this.properties.getRemote();
			String url = this.remoteUrl + remoteProperties.getContextPath() + "/debug";
			TunnelConnection connection = new HttpTunnelConnection(url, requestFactory);
			int localPort = remoteProperties.getDebug().getLocalPort();
			TunnelClient client = new TunnelClient(localPort, connection);
			client.addListener(new LoggingTunnelClientListener());
			return client;
		}

	}

}

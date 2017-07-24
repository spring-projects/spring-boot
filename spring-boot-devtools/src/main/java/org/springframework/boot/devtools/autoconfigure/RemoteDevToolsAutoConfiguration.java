/*
 * Copyright 2012-2017 the original author or authors.
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

import java.util.Collection;

import javax.servlet.Filter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.devtools.remote.server.AccessManager;
import org.springframework.boot.devtools.remote.server.Dispatcher;
import org.springframework.boot.devtools.remote.server.DispatcherFilter;
import org.springframework.boot.devtools.remote.server.Handler;
import org.springframework.boot.devtools.remote.server.HandlerMapper;
import org.springframework.boot.devtools.remote.server.HttpHeaderAccessManager;
import org.springframework.boot.devtools.remote.server.HttpStatusHandler;
import org.springframework.boot.devtools.remote.server.UrlHandlerMapper;
import org.springframework.boot.devtools.restart.server.DefaultSourceFolderUrlFilter;
import org.springframework.boot.devtools.restart.server.HttpRestartServer;
import org.springframework.boot.devtools.restart.server.HttpRestartServerHandler;
import org.springframework.boot.devtools.restart.server.SourceFolderUrlFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for remote development support.
 *
 * @author Phillip Webb
 * @author Rob Winch
 * @author Andy Wilkinson
 * @since 1.3.0
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.devtools.remote", name = "secret")
@ConditionalOnClass({ Filter.class, ServerHttpRequest.class })
@EnableConfigurationProperties({ ServerProperties.class, DevToolsProperties.class })
public class RemoteDevToolsAutoConfiguration {

	private static final Log logger = LogFactory
			.getLog(RemoteDevToolsAutoConfiguration.class);

	private final DevToolsProperties properties;

	private final ServerProperties serverProperties;

	public RemoteDevToolsAutoConfiguration(DevToolsProperties properties,
			ServerProperties serverProperties) {
		this.properties = properties;
		this.serverProperties = serverProperties;
	}

	@Bean
	@ConditionalOnMissingBean
	public AccessManager remoteDevToolsAccessManager() {
		RemoteDevToolsProperties remoteProperties = this.properties.getRemote();
		return new HttpHeaderAccessManager(remoteProperties.getSecretHeaderName(),
				remoteProperties.getSecret());
	}

	@Bean
	public HandlerMapper remoteDevToolsHealthCheckHandlerMapper() {
		Handler handler = new HttpStatusHandler();
		return new UrlHandlerMapper(
				(this.serverProperties.getServlet().getContextPath() == null ? ""
						: this.serverProperties.getServlet().getContextPath())
						+ this.properties.getRemote().getContextPath(),
				handler);
	}

	@Bean
	@ConditionalOnMissingBean
	public DispatcherFilter remoteDevToolsDispatcherFilter(AccessManager accessManager,
			Collection<HandlerMapper> mappers) {
		Dispatcher dispatcher = new Dispatcher(accessManager, mappers);
		return new DispatcherFilter(dispatcher);
	}

	/**
	 * Configuration for remote update and restarts.
	 */
	@ConditionalOnProperty(prefix = "spring.devtools.remote.restart", name = "enabled", matchIfMissing = true)
	static class RemoteRestartConfiguration {

		@Autowired
		private DevToolsProperties properties;

		@Autowired
		private ServerProperties serverProperties;

		@Bean
		@ConditionalOnMissingBean
		public SourceFolderUrlFilter remoteRestartSourceFolderUrlFilter() {
			return new DefaultSourceFolderUrlFilter();
		}

		@Bean
		@ConditionalOnMissingBean
		public HttpRestartServer remoteRestartHttpRestartServer(
				SourceFolderUrlFilter sourceFolderUrlFilter) {
			return new HttpRestartServer(sourceFolderUrlFilter);
		}

		@Bean
		@ConditionalOnMissingBean(name = "remoteRestartHandlerMapper")
		public UrlHandlerMapper remoteRestartHandlerMapper(HttpRestartServer server) {
			String url = (this.serverProperties.getServlet().getContextPath() == null ? ""
					: this.serverProperties.getServlet().getContextPath())
					+ this.properties.getRemote().getContextPath() + "/restart";
			logger.warn("Listening for remote restart updates on " + url);
			Handler handler = new HttpRestartServerHandler(server);
			return new UrlHandlerMapper(url, handler);
		}

	}

	@Configuration
	@ConditionalOnClass(WebSecurityConfigurerAdapter.class)
	@ConditionalOnBean(ObjectPostProcessor.class)
	static class RemoteDevToolsSecurityConfiguration {

		@Bean
		public RemoteRestartWebSecurityConfigurer remoteRestartWebSecurityConfigurer() {
			return new RemoteRestartWebSecurityConfigurer();
		}

		@Order(SecurityProperties.IGNORED_ORDER + 2)
		static class RemoteRestartWebSecurityConfigurer
				extends WebSecurityConfigurerAdapter {

			@Autowired
			private DevToolsProperties properties;

			@Override
			public void configure(HttpSecurity http) throws Exception {
				http.antMatcher(this.properties.getRemote().getContextPath() + "/**");
				http.csrf().disable();
			}

		}

	}

}

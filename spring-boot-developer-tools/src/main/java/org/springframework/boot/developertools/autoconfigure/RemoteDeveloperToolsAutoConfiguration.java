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

import java.util.Collection;

import javax.servlet.Filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.developertools.remote.server.AccessManager;
import org.springframework.boot.developertools.remote.server.Dispatcher;
import org.springframework.boot.developertools.remote.server.DispatcherFilter;
import org.springframework.boot.developertools.remote.server.Handler;
import org.springframework.boot.developertools.remote.server.HandlerMapper;
import org.springframework.boot.developertools.remote.server.HttpStatusHandler;
import org.springframework.boot.developertools.remote.server.UrlHandlerMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for remote development support.
 *
 * @author Phillip Webb
 * @author Rob Winch
 * @since 1.3.0
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.developertools.remote", name = "enabled")
@ConditionalOnClass({ Filter.class, ServerHttpRequest.class })
@EnableConfigurationProperties(DeveloperToolsProperties.class)
public class RemoteDeveloperToolsAutoConfiguration {

	@Autowired
	private DeveloperToolsProperties properties;

	@Bean
	public HandlerMapper remoteDeveloperToolsHealthCheckHandlerMapper() {
		Handler handler = new HttpStatusHandler();
		return new UrlHandlerMapper(this.properties.getRemote().getContextPath(), handler);
	}

	@Bean
	@ConditionalOnMissingBean
	public DispatcherFilter remoteDeveloperToolsDispatcherFilter(
			Collection<HandlerMapper> mappers) {
		Dispatcher dispatcher = new Dispatcher(AccessManager.PERMIT_ALL, mappers);
		return new DispatcherFilter(dispatcher);
	}

}

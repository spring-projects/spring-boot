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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.developertools.autoconfigure.DeveloperToolsProperties;
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

}

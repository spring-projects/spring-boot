/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the
 * {@link ManagementServerProperties} bean.
 *
 * @author Dave Syer
 */
@Configuration
@AutoConfigureAfter(ServerPropertiesAutoConfiguration.class)
@EnableConfigurationProperties
public class ManagementServerPropertiesAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public ManagementServerProperties managementServerProperties() {
		return new ManagementServerProperties();
	}

	// In case security auto configuration hasn't been included
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnClass(name = "org.springframework.security.config.annotation.web.configuration.EnableWebSecurity")
	public SecurityProperties securityProperties() {
		return new SecurityProperties();
	}

	// In case server auto configuration hasn't been included
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnWebApplication
	public ServerProperties serverProperties() {
		return new ServerProperties();
	}

}

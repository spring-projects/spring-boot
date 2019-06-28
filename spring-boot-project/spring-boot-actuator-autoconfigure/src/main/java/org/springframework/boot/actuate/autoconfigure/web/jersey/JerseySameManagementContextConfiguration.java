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
package org.springframework.boot.actuate.autoconfigure.web.jersey;

import org.glassfish.jersey.server.ResourceConfig;

import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.jersey.JerseyProperties;
import org.springframework.boot.autoconfigure.web.servlet.DefaultJerseyApplicationPath;
import org.springframework.boot.autoconfigure.web.servlet.JerseyApplicationPath;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * {@link ManagementContextConfiguration @ManagementContextConfiguration} for Jersey
 * infrastructure when the management context is the same as the main application context.
 *
 * @author Madhura Bhave
 */
@ManagementContextConfiguration(ManagementContextType.SAME)
@ConditionalOnMissingBean(ResourceConfig.class)
@Import(JerseyManagementContextConfiguration.class)
@EnableConfigurationProperties(JerseyProperties.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(ResourceConfig.class)
@ConditionalOnMissingClass("org.springframework.web.servlet.DispatcherServlet")
public class JerseySameManagementContextConfiguration {

	@Bean
	@ConditionalOnMissingBean(JerseyApplicationPath.class)
	public JerseyApplicationPath jerseyApplicationPath(JerseyProperties properties, ResourceConfig config) {
		return new DefaultJerseyApplicationPath(properties.getApplicationPath(), config);
	}

}

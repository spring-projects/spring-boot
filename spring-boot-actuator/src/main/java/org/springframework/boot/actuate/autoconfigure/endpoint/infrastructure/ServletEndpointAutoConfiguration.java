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

package org.springframework.boot.actuate.autoconfigure.endpoint.infrastructure;

import javax.servlet.Servlet;

import org.springframework.boot.actuate.autoconfigure.endpoint.ManagementContextResolver;
import org.springframework.boot.actuate.autoconfigure.web.ManagementServerProperties;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.hateoas.HypermediaAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.filter.ApplicationContextHeaderFilter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Servlet-specific endpoint
 * concerns.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Christian Dupuis
 * @author Andy Wilkinson
 * @author Johannes Edmeier
 * @author Eddú Meléndez
 * @author Venil Noronha
 * @author Madhura Bhave
 */
@Configuration
@ConditionalOnClass(Servlet.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
@EnableConfigurationProperties(ManagementServerProperties.class)
@AutoConfigureAfter({ PropertyPlaceholderAutoConfiguration.class,
		ServletWebServerFactoryAutoConfiguration.class, WebMvcAutoConfiguration.class,
		RepositoryRestMvcAutoConfiguration.class, HypermediaAutoConfiguration.class,
		HttpMessageConvertersAutoConfiguration.class,
		EndpointInfrastructureAutoConfiguration.class })
public class ServletEndpointAutoConfiguration {

	@Bean
	public ManagementContextResolver managementContextResolver(
			ApplicationContext applicationContext) {
		return new ManagementContextResolver(applicationContext);
	}

	// Put Servlets and Filters in their own nested class so they don't force early
	// instantiation of ManagementServerProperties.
	@Configuration
	@ConditionalOnProperty(prefix = "management", name = "add-application-context-header", havingValue = "true")
	protected static class ApplicationContextFilterConfiguration {

		@Bean
		public ApplicationContextHeaderFilter applicationContextIdFilter(
				ApplicationContext context) {
			return new ApplicationContextHeaderFilter(context);
		}

	}

}

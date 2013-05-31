/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.bootstrap.actuate.autoconfigure;

import org.springframework.bootstrap.actuate.properties.EndpointsProperties;
import org.springframework.bootstrap.actuate.properties.ManagementServerProperties;
import org.springframework.bootstrap.context.annotation.ConditionalOnMissingBean;
import org.springframework.bootstrap.context.annotation.EnableAutoConfiguration;
import org.springframework.bootstrap.context.annotation.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for service apps.
 * 
 * @author Dave Syer
 */
@Configuration
@Import({ ActuatorWebConfiguration.class, MetricRepositoryConfiguration.class,
		ErrorConfiguration.class, TraceFilterConfiguration.class,
		MetricFilterConfiguration.class, AuditConfiguration.class })
public class ActuatorAutoConfiguration {

	// ServerProperties has to be declared in a non-conditional bean, so that it gets
	// added to the context early enough

	@EnableConfigurationProperties
	public static class ServerPropertiesConfiguration {

		@ConditionalOnMissingBean(ManagementServerProperties.class)
		@Bean(name = "org.springframework.bootstrap.actuate.properties.ManagementServerProperties")
		public ManagementServerProperties managementServerProperties() {
			return new ManagementServerProperties();
		}

		@Bean
		@ConditionalOnMissingBean(EndpointsProperties.class)
		public EndpointsProperties endpointsProperties() {
			return new EndpointsProperties();
		}

	}

}

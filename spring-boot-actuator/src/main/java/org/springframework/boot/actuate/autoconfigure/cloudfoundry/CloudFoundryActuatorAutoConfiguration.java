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

package org.springframework.boot.actuate.autoconfigure.cloudfoundry;

import org.springframework.boot.actuate.autoconfigure.endpoint.infrastructure.ServletEndpointAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnCloudPlatform;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.IgnoredRequestCustomizer;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to expose actuator endpoints for
 * cloud foundry to use.
 *
 * @author Madhura Bhave
 * @since 2.0.0
 */
@Configuration
@ConditionalOnProperty(prefix = "management.cloudfoundry", name = "enabled", matchIfMissing = true)
@AutoConfigureAfter(ServletEndpointAutoConfiguration.class)
@ConditionalOnCloudPlatform(CloudPlatform.CLOUD_FOUNDRY)
public class CloudFoundryActuatorAutoConfiguration {

	/**
	 * Nested configuration for ignored requests if Spring Security is present.
	 */
	@ConditionalOnClass(WebSecurity.class)
	static class CloudFoundryIgnoredRequestConfiguration {

		@Bean
		public IgnoredRequestCustomizer cloudFoundryIgnoredRequestCustomizer() {
			return new CloudFoundryIgnoredRequestCustomizer();
		}

		private static class CloudFoundryIgnoredRequestCustomizer
				implements IgnoredRequestCustomizer {

			@Override
			public void customize(WebSecurity.IgnoredRequestConfigurer configurer) {
				configurer.requestMatchers(
						new AntPathRequestMatcher("/cloudfoundryapplication/**"));
			}

		}

	}

}

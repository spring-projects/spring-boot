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

import org.springframework.boot.actuate.autoconfigure.cloudfoundry.reactive.CloudFoundryReactiveHealthEndpointWebExtension;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.reactive.ReactiveCloudFoundryActuatorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.servlet.CloudFoundryActuatorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.servlet.CloudFoundryHealthEndpointWebExtension;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnEnabledEndpoint;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HealthEndpointWebExtension;
import org.springframework.boot.actuate.health.ReactiveHealthEndpointWebExtension;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnCloudPlatform;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Cloud Foundry Health endpoint extensions.
 *
 * @author Madhura Bhave
 */
@Configuration
@ConditionalOnCloudPlatform(CloudPlatform.CLOUD_FOUNDRY)
@AutoConfigureBefore({ ReactiveCloudFoundryActuatorAutoConfiguration.class,
		CloudFoundryActuatorAutoConfiguration.class })
@AutoConfigureAfter(HealthEndpointAutoConfiguration.class)
public class CloudFoundryHealthWebEndpointAutoConfiguration {

	@Configuration
	@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
	static class ServletWebHealthConfiguration {

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnEnabledEndpoint
		@ConditionalOnBean({ HealthEndpoint.class, HealthEndpointWebExtension.class })
		public CloudFoundryHealthEndpointWebExtension cloudFoundryHealthEndpointWebExtension(
				HealthEndpointWebExtension healthEndpointWebExtension) {
			return new CloudFoundryHealthEndpointWebExtension(healthEndpointWebExtension);
		}

	}

	@Configuration
	@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
	static class ReactiveWebHealthConfiguration {

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnEnabledEndpoint
		@ConditionalOnBean({ HealthEndpoint.class,
				ReactiveHealthEndpointWebExtension.class })
		public CloudFoundryReactiveHealthEndpointWebExtension cloudFoundryReactiveHealthEndpointWebExtension(
				ReactiveHealthEndpointWebExtension reactiveHealthEndpointWebExtension) {
			return new CloudFoundryReactiveHealthEndpointWebExtension(
					reactiveHealthEndpointWebExtension);
		}

	}

}

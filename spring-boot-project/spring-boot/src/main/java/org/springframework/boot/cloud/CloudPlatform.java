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

package org.springframework.boot.cloud;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.StringUtils;

/**
 * Simple detection for well known cloud platforms. For more advanced cloud provider
 * integration consider the Spring Cloud project.
 *
 * @author Phillip Webb
 * @since 1.3.0
 * @see "http://cloud.spring.io"
 */
public enum CloudPlatform {

	/**
	 * Cloud Foundry platform.
	 */
	CLOUD_FOUNDRY {

		@Override
		public boolean isActive(Environment environment) {
			return environment.containsProperty("VCAP_APPLICATION")
					|| environment.containsProperty("VCAP_SERVICES");
		}

	},

	/**
	 * Heroku platform.
	 */
	HEROKU {

		@Override
		public boolean isActive(Environment environment) {
			return environment.containsProperty("DYNO");
		}

	},

	/**
	 * SAP Cloud platform.
	 */
	SAP {

		@Override
		public boolean isActive(Environment environment) {
			return environment.containsProperty("HC_LANDSCAPE");
		}

	},

	/**
	 * Kubernetes platform.
	 */
	KUBERNETES {
		@Override
		public boolean isActive(Environment environment) {
			if (environment instanceof ConfigurableEnvironment) {
				MapPropertySource propertySource = (MapPropertySource) ((ConfigurableEnvironment) environment)
						.getPropertySources()
						.get(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
				if (propertySource != null) {
					for (String name : propertySource.getPropertyNames()) {
						if (name.endsWith("_SERVICE_HOST")) {
							String serviceName = StringUtils.split(name,
									"_SERVICE_HOST")[0];
							if (propertySource
									.getProperty(serviceName + "_SERVICE_PORT") != null) {
								return true;
							}
						}
					}
				}
			}
			return false;
		}
	};

	/**
	 * Determines if the platform is active (i.e. the application is running in it).
	 * @param environment the environment
	 * @return if the platform is active.
	 */
	public abstract boolean isActive(Environment environment);

	/**
	 * Returns if the platform is behind a load balancer and uses
	 * {@literal X-Forwarded-For} headers.
	 * @return if {@literal X-Forwarded-For} headers are used
	 */
	public boolean isUsingForwardHeaders() {
		return true;
	}

	/**
	 * Returns the active {@link CloudPlatform} or {@code null} if one cannot be deduced.
	 * @param environment the environment
	 * @return the {@link CloudPlatform} or {@code null}
	 */
	public static CloudPlatform getActive(Environment environment) {
		if (environment != null) {
			for (CloudPlatform cloudPlatform : values()) {
				if (cloudPlatform.isActive(environment)) {
					return cloudPlatform;
				}
			}
		}
		return null;
	}

}

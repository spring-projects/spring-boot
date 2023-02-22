/*
 * Copyright 2012-2023 the original author or authors.
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

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;

/**
 * Simple detection for well known cloud platforms. Detection can be forced using the
 * {@code "spring.main.cloud-platform"} configuration property.
 *
 * @author Phillip Webb
 * @author Brian Clozel
 * @author Nguyen Sach
 * @since 1.3.0
 */
public enum CloudPlatform {

	/**
	 * No Cloud platform. Useful when false-positives are detected.
	 */
	NONE {

		@Override
		public boolean isDetected(Environment environment) {
			return false;
		}

	},

	/**
	 * Cloud Foundry platform.
	 */
	CLOUD_FOUNDRY {

		@Override
		public boolean isDetected(Environment environment) {
			return environment.containsProperty("VCAP_APPLICATION") || environment.containsProperty("VCAP_SERVICES");
		}

	},

	/**
	 * Heroku platform.
	 */
	HEROKU {

		@Override
		public boolean isDetected(Environment environment) {
			return environment.containsProperty("DYNO");
		}

	},

	/**
	 * SAP Cloud platform.
	 */
	SAP {

		@Override
		public boolean isDetected(Environment environment) {
			return environment.containsProperty("HC_LANDSCAPE");
		}

	},

	/**
	 * Kubernetes platform.
	 */
	KUBERNETES {

		private static final String KUBERNETES_SERVICE_HOST = "KUBERNETES_SERVICE_HOST";

		private static final String KUBERNETES_SERVICE_PORT = "KUBERNETES_SERVICE_PORT";

		private static final String SERVICE_HOST_SUFFIX = "_SERVICE_HOST";

		private static final String SERVICE_PORT_SUFFIX = "_SERVICE_PORT";

		@Override
		public boolean isDetected(Environment environment) {
			if (environment instanceof ConfigurableEnvironment) {
				return isAutoDetected((ConfigurableEnvironment) environment);
			}
			return false;
		}

		private boolean isAutoDetected(ConfigurableEnvironment environment) {
			PropertySource<?> environmentPropertySource = environment.getPropertySources()
				.get(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
			if (environmentPropertySource != null) {
				if (environmentPropertySource.containsProperty(KUBERNETES_SERVICE_HOST)
						&& environmentPropertySource.containsProperty(KUBERNETES_SERVICE_PORT)) {
					return true;
				}
				if (environmentPropertySource instanceof EnumerablePropertySource) {
					return isAutoDetected((EnumerablePropertySource<?>) environmentPropertySource);
				}
			}
			return false;
		}

		private boolean isAutoDetected(EnumerablePropertySource<?> environmentPropertySource) {
			for (String propertyName : environmentPropertySource.getPropertyNames()) {
				if (propertyName.endsWith(SERVICE_HOST_SUFFIX)) {
					String serviceName = propertyName.substring(0,
							propertyName.length() - SERVICE_HOST_SUFFIX.length());
					if (environmentPropertySource.getProperty(serviceName + SERVICE_PORT_SUFFIX) != null) {
						return true;
					}
				}
			}
			return false;
		}

	},

	/**
	 * Azure App Service platform.
	 */
	AZURE_APP_SERVICE {

		private final List<String> azureEnvVariables = Arrays.asList("WEBSITE_SITE_NAME", "WEBSITE_INSTANCE_ID",
				"WEBSITE_RESOURCE_GROUP", "WEBSITE_SKU");

		@Override
		public boolean isDetected(Environment environment) {
			return this.azureEnvVariables.stream().allMatch(environment::containsProperty);
		}

	};

	private static final String PROPERTY_NAME = "spring.main.cloud-platform";

	/**
	 * Determines if the platform is active (i.e. the application is running in it).
	 * @param environment the environment
	 * @return if the platform is active.
	 */
	public boolean isActive(Environment environment) {
		String platformProperty = environment.getProperty(PROPERTY_NAME);
		return isEnforced(platformProperty) || (platformProperty == null && isDetected(environment));
	}

	/**
	 * Determines if the platform is enforced by looking at the
	 * {@code "spring.main.cloud-platform"} configuration property.
	 * @param environment the environment
	 * @return if the platform is enforced
	 * @since 2.3.0
	 */
	public boolean isEnforced(Environment environment) {
		return isEnforced(environment.getProperty(PROPERTY_NAME));
	}

	/**
	 * Determines if the platform is enforced by looking at the
	 * {@code "spring.main.cloud-platform"} configuration property.
	 * @param binder the binder
	 * @return if the platform is enforced
	 * @since 2.4.0
	 */
	public boolean isEnforced(Binder binder) {
		return isEnforced(binder.bind(PROPERTY_NAME, String.class).orElse(null));
	}

	private boolean isEnforced(String platform) {
		return name().equalsIgnoreCase(platform);
	}

	/**
	 * Determines if the platform is detected by looking for platform-specific environment
	 * variables.
	 * @param environment the environment
	 * @return if the platform is auto-detected.
	 * @since 2.3.0
	 */
	public abstract boolean isDetected(Environment environment);

	/**
	 * Returns if the platform is behind a load balancer and uses
	 * {@literal X-Forwarded-For} headers.
	 * @return if {@literal X-Forwarded-For} headers are used
	 */
	public boolean isUsingForwardHeaders() {
		return true;
	}

	/**
	 * Returns the active {@link CloudPlatform} or {@code null} if one is not active.
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

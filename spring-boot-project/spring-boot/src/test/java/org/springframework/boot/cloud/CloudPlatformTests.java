/*
 * Copyright 2012-2020 the original author or authors.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MockConfigurationPropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CloudPlatform}.
 *
 * @author Phillip Webb
 */
class CloudPlatformTests {

	@Test
	void getActiveWhenEnvironmentIsNullShouldReturnNull() {
		CloudPlatform platform = CloudPlatform.getActive(null);
		assertThat(platform).isNull();
	}

	@Test
	void getActiveWhenNotInCloudShouldReturnNull() {
		Environment environment = new MockEnvironment();
		CloudPlatform platform = CloudPlatform.getActive(environment);
		assertThat(platform).isNull();

	}

	@Test
	void getActiveWhenHasVcapApplicationShouldReturnCloudFoundry() {
		Environment environment = new MockEnvironment().withProperty("VCAP_APPLICATION", "---");
		CloudPlatform platform = CloudPlatform.getActive(environment);
		assertThat(platform).isEqualTo(CloudPlatform.CLOUD_FOUNDRY);
		assertThat(platform.isActive(environment)).isTrue();
	}

	@Test
	void getActiveWhenHasVcapServicesShouldReturnCloudFoundry() {
		Environment environment = new MockEnvironment().withProperty("VCAP_SERVICES", "---");
		CloudPlatform platform = CloudPlatform.getActive(environment);
		assertThat(platform).isEqualTo(CloudPlatform.CLOUD_FOUNDRY);
		assertThat(platform.isActive(environment)).isTrue();
	}

	@Test
	void getActiveWhenHasDynoShouldReturnHeroku() {
		Environment environment = new MockEnvironment().withProperty("DYNO", "---");
		CloudPlatform platform = CloudPlatform.getActive(environment);
		assertThat(platform).isEqualTo(CloudPlatform.HEROKU);
		assertThat(platform.isActive(environment)).isTrue();
	}

	@Test
	void getActiveWhenHasHcLandscapeShouldReturnSap() {
		Environment environment = new MockEnvironment().withProperty("HC_LANDSCAPE", "---");
		CloudPlatform platform = CloudPlatform.getActive(environment);
		assertThat(platform).isEqualTo(CloudPlatform.SAP);
		assertThat(platform.isActive(environment)).isTrue();
	}

	@Test
	void getActiveWhenHasKubernetesServiceHostAndPortShouldReturnKubernetes() {
		Map<String, Object> envVars = new HashMap<>();
		envVars.put("KUBERNETES_SERVICE_HOST", "---");
		envVars.put("KUBERNETES_SERVICE_PORT", "8080");
		Environment environment = getEnvironmentWithEnvVariables(envVars);
		CloudPlatform platform = CloudPlatform.getActive(environment);
		assertThat(platform).isEqualTo(CloudPlatform.KUBERNETES);
		assertThat(platform.isActive(environment)).isTrue();
	}

	@Test
	void getActiveWhenHasKubernetesServiceHostAndNoKubernetesServicePortShouldNotReturnKubernetes() {
		Environment environment = getEnvironmentWithEnvVariables(
				Collections.singletonMap("KUBERNETES_SERVICE_HOST", "---"));
		CloudPlatform platform = CloudPlatform.getActive(environment);
		assertThat(platform).isNull();
	}

	@Test
	void getActiveWhenHasKubernetesServicePortAndNoKubernetesServiceHostShouldNotReturnKubernetes() {
		Environment environment = getEnvironmentWithEnvVariables(
				Collections.singletonMap("KUBERNETES_SERVICE_PORT", "8080"));
		CloudPlatform platform = CloudPlatform.getActive(environment);
		assertThat(platform).isNull();
	}

	@Test
	void getActiveWhenHasServiceHostAndServicePortShouldReturnKubernetes() {
		Map<String, Object> envVars = new HashMap<>();
		envVars.put("EXAMPLE_SERVICE_HOST", "---");
		envVars.put("EXAMPLE_SERVICE_PORT", "8080");
		Environment environment = getEnvironmentWithEnvVariables(envVars);
		CloudPlatform platform = CloudPlatform.getActive(environment);
		assertThat(platform).isEqualTo(CloudPlatform.KUBERNETES);
		assertThat(platform.isActive(environment)).isTrue();
	}

	@Test
	void getActiveWhenHasServiceHostAndNoServicePortShouldNotReturnKubernetes() {
		Environment environment = getEnvironmentWithEnvVariables(
				Collections.singletonMap("EXAMPLE_SERVICE_HOST", "---"));
		CloudPlatform platform = CloudPlatform.getActive(environment);
		assertThat(platform).isNull();
	}

	@Test
	void getActiveWhenHasEnforcedCloudPlatform() {
		Environment environment = getEnvironmentWithEnvVariables(
				Collections.singletonMap("spring.main.cloud-platform", "kubernetes"));
		CloudPlatform platform = CloudPlatform.getActive(environment);
		assertThat(platform).isEqualTo(CloudPlatform.KUBERNETES);
	}

	@Test
	void isEnforcedWhenEnvironmentPropertyMatchesReturnsTrue() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.main.cloud-platform", "kubernetes");
		assertThat(CloudPlatform.KUBERNETES.isEnforced(environment)).isTrue();
	}

	@Test
	void isEnforcedWhenEnvironmentPropertyDoesNotMatchReturnsFalse() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.main.cloud-platform", "heroku");
		assertThat(CloudPlatform.KUBERNETES.isEnforced(environment)).isFalse();
	}

	@Test
	void isEnforcedWhenEnvironmentPropertyIsMissingatchReturnsFalse() {
		MockEnvironment environment = new MockEnvironment();
		assertThat(CloudPlatform.KUBERNETES.isEnforced(environment)).isFalse();
	}

	@Test
	void isEnforcedWhenBinderPropertyMatchesReturnsTrue() {
		Binder binder = new Binder(new MockConfigurationPropertySource("spring.main.cloud-platform", "kubernetes"));
		assertThat(CloudPlatform.KUBERNETES.isEnforced(binder)).isTrue();
	}

	@Test
	void isEnforcedWhenBinderPropertyDoesNotMatchReturnsFalse() {
		Binder binder = new Binder(new MockConfigurationPropertySource("spring.main.cloud-platform", "heroku"));
		assertThat(CloudPlatform.KUBERNETES.isEnforced(binder)).isFalse();
	}

	@Test
	void isEnforcedWhenBinderPropertyIsMissingatchReturnsFalse() {
		Binder binder = new Binder(new MockConfigurationPropertySource());
		assertThat(CloudPlatform.KUBERNETES.isEnforced(binder)).isFalse();
	}

	private Environment getEnvironmentWithEnvVariables(Map<String, Object> environmentVariables) {
		MockEnvironment environment = new MockEnvironment();
		PropertySource<?> propertySource = new SystemEnvironmentPropertySource(
				StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, environmentVariables);
		environment.getPropertySources().addFirst(propertySource);
		return environment;
	}

}

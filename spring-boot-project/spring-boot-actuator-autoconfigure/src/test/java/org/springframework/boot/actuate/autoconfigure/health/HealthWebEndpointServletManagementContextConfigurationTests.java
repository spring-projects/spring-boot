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

package org.springframework.boot.actuate.autoconfigure.health;

import java.util.Map;

import org.junit.Test;

import org.springframework.boot.actuate.health.HealthStatusHttpMapper;
import org.springframework.boot.actuate.health.HealthWebEndpointExtension;
import org.springframework.boot.actuate.health.StatusWebEndpointExtension;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HealthWebEndpointManagementContextConfiguration} in a servlet
 * environment.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
public class HealthWebEndpointServletManagementContextConfigurationTests {

	private WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withUserConfiguration(HealthIndicatorAutoConfiguration.class,
					HealthEndpointAutoConfiguration.class,
					HealthWebEndpointManagementContextConfiguration.class);

	@Test
	public void runShouldCreateExtensionBeans() throws Exception {
		this.contextRunner.run((context) -> assertThat(context)
				.hasSingleBean(StatusWebEndpointExtension.class)
				.hasSingleBean(HealthWebEndpointExtension.class));
	}

	@Test
	public void runWhenHealthEndpointIsDisabledShouldNotCreateExtensionBeans()
			throws Exception {
		this.contextRunner.withPropertyValues("endpoints.health.enabled:false")
				.run((context) -> assertThat(context)
						.doesNotHaveBean(HealthWebEndpointExtension.class));
	}

	@Test
	public void runWhenStatusEndpointIsDisabledShouldNotCreateExtensionBeans()
			throws Exception {
		this.contextRunner.withPropertyValues("endpoints.status.enabled:false")
				.run((context) -> assertThat(context)
						.doesNotHaveBean(StatusWebEndpointExtension.class));
	}

	@Test
	public void runWithCustomHealthMappingShouldMapStatusCode() throws Exception {
		this.contextRunner
				.withPropertyValues("management.health.status.http-mapping.CUSTOM=500")
				.run((context) -> {
					Object extension = context.getBean(HealthWebEndpointExtension.class);
					HealthStatusHttpMapper mapper = (HealthStatusHttpMapper) ReflectionTestUtils
							.getField(extension, "statusHttpMapper");
					Map<String, Integer> statusMappings = mapper.getStatusMapping();
					assertThat(statusMappings).containsEntry("DOWN", 503);
					assertThat(statusMappings).containsEntry("OUT_OF_SERVICE", 503);
					assertThat(statusMappings).containsEntry("CUSTOM", 500);
				});
	}

	@Test
	public void runWithCustomStatusMappingShouldMapStatusCode() throws Exception {
		this.contextRunner
				.withPropertyValues("management.health.status.http-mapping.CUSTOM=500")
				.run((context) -> {
					Object extension = context.getBean(StatusWebEndpointExtension.class);
					HealthStatusHttpMapper mapper = (HealthStatusHttpMapper) ReflectionTestUtils
							.getField(extension, "statusHttpMapper");
					Map<String, Integer> statusMappings = mapper.getStatusMapping();
					assertThat(statusMappings).containsEntry("DOWN", 503);
					assertThat(statusMappings).containsEntry("OUT_OF_SERVICE", 503);
					assertThat(statusMappings).containsEntry("CUSTOM", 500);
				});
	}

}

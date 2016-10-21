/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.actuate.cloudfoundry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.actuate.autoconfigure.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.EndpointWebMvcAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.EndpointWebMvcManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.ManagementServerPropertiesAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CloudFoundryActuatorAutoConfiguration}.
 *
 * @author Madhura Bhave
 */
public class CloudFoundryActuatorAutoConfigurationTests {

	private AnnotationConfigWebApplicationContext context;

	@Before
	public void setUp() {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(SecurityAutoConfiguration.class,
				WebMvcAutoConfiguration.class,
				ManagementWebSecurityAutoConfiguration.class,
				JacksonAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				EndpointAutoConfiguration.class, EndpointWebMvcAutoConfiguration.class,
				ManagementServerPropertiesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				EndpointWebMvcManagementContextConfiguration.class,
				CloudFoundryActuatorAutoConfiguration.class);
	}

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void cloudFoundryPlatformActive() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context, "VCAP_APPLICATION:---",
				"management.cloudfoundry.enabled:true");
		this.context.refresh();
		CloudFoundryEndpointHandlerMapping handlerMapping = this.context.getBean(
				"cloudFoundryEndpointHandlerMapping",
				CloudFoundryEndpointHandlerMapping.class);
		assertThat(handlerMapping.getPrefix()).isEqualTo("/cloudfoundryapplication");
	}

	@Test
	public void cloudFoundryPlatformInactive() throws Exception {
		this.context.refresh();
		assertThat(this.context.containsBean("cloudFoundryEndpointHandlerMapping"))
				.isFalse();
	}

	@Test
	public void cloudFoundryManagementEndpointsDisabled() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context, "VCAP_APPLICATION=---",
				"management.cloudfoundry.enabled:false");
		this.context.refresh();
		assertThat(this.context.containsBean("cloudFoundryEndpointHandlerMapping"))
				.isFalse();
	}

}

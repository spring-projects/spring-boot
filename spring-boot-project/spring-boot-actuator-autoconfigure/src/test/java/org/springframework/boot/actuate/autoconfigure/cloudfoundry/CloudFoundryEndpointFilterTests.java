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

import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.reflect.OperationMethodInvokerAdvisor;
import org.springframework.boot.actuate.endpoint.reflect.ParameterMapper;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.EndpointPathResolver;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.boot.actuate.endpoint.web.annotation.WebAnnotationEndpointDiscoverer;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CloudFoundryEndpointFilter}.
 *
 * @author Madhura Bhave
 */
public class CloudFoundryEndpointFilterTests {

	private CloudFoundryEndpointFilter filter;

	@Before
	public void setUp() {
		this.filter = new CloudFoundryEndpointFilter();
	}

	@Test
	public void matchIfDiscovererCloudFoundryShouldReturnFalse() {
		CloudFoundryWebAnnotationEndpointDiscoverer discoverer = Mockito
				.mock(CloudFoundryWebAnnotationEndpointDiscoverer.class);
		assertThat(this.filter.match(null, discoverer)).isTrue();
	}

	@Test
	public void matchIfDiscovererNotCloudFoundryShouldReturnFalse() {
		WebAnnotationEndpointDiscoverer discoverer = Mockito
				.mock(WebAnnotationEndpointDiscoverer.class);
		assertThat(this.filter.match(null, discoverer)).isFalse();
	}

	static class TestEndpointDiscoverer extends WebAnnotationEndpointDiscoverer {

		TestEndpointDiscoverer(ApplicationContext applicationContext,
				ParameterMapper parameterMapper, EndpointMediaTypes endpointMediaTypes,
				EndpointPathResolver endpointPathResolver,
				Collection<? extends OperationMethodInvokerAdvisor> invokerAdvisors,
				Collection<? extends EndpointFilter<WebOperation>> filters) {
			super(applicationContext, parameterMapper, endpointMediaTypes,
					endpointPathResolver, invokerAdvisors, filters);
		}

	}

}

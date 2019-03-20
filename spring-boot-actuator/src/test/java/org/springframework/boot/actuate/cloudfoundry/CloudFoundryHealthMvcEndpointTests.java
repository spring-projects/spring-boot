/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.cloudfoundry;

import org.junit.Test;

import org.springframework.boot.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link CloudFoundryHealthMvcEndpoint}.
 *
 * @author Madhura Bhave
 */
public class CloudFoundryHealthMvcEndpointTests {

	@Test
	public void cloudFoundryHealthEndpointShouldAlwaysReturnAllHealthDetails()
			throws Exception {
		HealthEndpoint endpoint = mock(HealthEndpoint.class);
		given(endpoint.isEnabled()).willReturn(true);
		CloudFoundryHealthMvcEndpoint mvc = new CloudFoundryHealthMvcEndpoint(endpoint);
		given(endpoint.invoke())
				.willReturn(new Health.Builder().up().withDetail("foo", "bar").build());
		given(endpoint.isSensitive()).willReturn(false);
		Object result = mvc.invoke(null, null);
		assertThat(result instanceof Health).isTrue();
		assertThat(((Health) result).getStatus() == Status.UP).isTrue();
		assertThat(((Health) result).getDetails().get("foo")).isEqualTo("bar");
	}

}

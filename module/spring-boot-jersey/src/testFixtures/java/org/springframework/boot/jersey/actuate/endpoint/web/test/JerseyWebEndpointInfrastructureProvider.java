/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.jersey.actuate.endpoint.web.test;

import java.util.List;

import org.springframework.boot.actuate.endpoint.web.test.WebEndpointInfrastructureProvider;
import org.springframework.boot.actuate.endpoint.web.test.WebEndpointTest.Infrastructure;

/**
 * {@link WebEndpointInfrastructureProvider} for Jersey.
 *
 * @author Stephane Nicoll
 */
class JerseyWebEndpointInfrastructureProvider implements WebEndpointInfrastructureProvider {

	@Override
	public boolean supports(Infrastructure infrastructure) {
		return infrastructure == Infrastructure.JERSEY;
	}

	@Override
	public List<Class<?>> getInfrastructureConfiguration(Infrastructure infrastructure) {
		return List.of(JerseyEndpointConfiguration.class);
	}

}

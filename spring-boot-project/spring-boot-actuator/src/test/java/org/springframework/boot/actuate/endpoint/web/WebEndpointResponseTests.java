/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.web;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebEndpointResponse}.
 *
 * @author Phillip Webb
 */
public class WebEndpointResponseTests {

	@Test
	public void createWithNoParamsShouldReturn200() {
		WebEndpointResponse<Object> response = new WebEndpointResponse<>();
		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getBody()).isNull();
	}

	@Test
	public void createWithStatusShouldReturnStatus() {
		WebEndpointResponse<Object> response = new WebEndpointResponse<>(404);
		assertThat(response.getStatus()).isEqualTo(404);
		assertThat(response.getBody()).isNull();
	}

	@Test
	public void createWithBodyShouldReturnBody() {
		WebEndpointResponse<Object> response = new WebEndpointResponse<>("body");
		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getBody()).isEqualTo("body");
	}

	@Test
	public void createWithBodyAndStatusShouldReturnStatusAndBody() {
		WebEndpointResponse<Object> response = new WebEndpointResponse<>("body", 500);
		assertThat(response.getStatus()).isEqualTo(500);
		assertThat(response.getBody()).isEqualTo("body");
	}

}

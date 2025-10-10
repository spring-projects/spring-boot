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

package org.springframework.boot.docs.testing.springbootapplications.autoconfiguredrestclient

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.response.MockRestResponseCreators

@RestClientTest(RemoteVehicleDetailsService::class)
class MyRestClientServiceTests(
	@Autowired val service: RemoteVehicleDetailsService,
	@Autowired val server: MockRestServiceServer) {

	@Test
	fun getVehicleDetailsWhenResultIsSuccessShouldReturnDetails() {
		server.expect(MockRestRequestMatchers.requestTo("https://example.com/greet/details"))
			.andRespond(MockRestResponseCreators.withSuccess("hello", MediaType.TEXT_PLAIN))
		val greeting = service.callRestService()
		assertThat(greeting).isEqualTo("hello")
	}

}


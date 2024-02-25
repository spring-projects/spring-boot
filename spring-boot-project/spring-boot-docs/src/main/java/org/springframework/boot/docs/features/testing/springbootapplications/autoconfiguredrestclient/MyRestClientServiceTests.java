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

package org.springframework.boot.docs.features.testing.springbootapplications.autoconfiguredrestclient;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * MyRestClientServiceTests class.
 */
@RestClientTest(RemoteVehicleDetailsService.class)
class MyRestClientServiceTests {

	@Autowired
	private RemoteVehicleDetailsService service;

	@Autowired
	private MockRestServiceServer server;

	/**
     * Test case to verify the behavior of the getVehicleDetailsWhenResultIsSuccessShouldReturnDetails method.
     * 
     * This method tests the functionality of the MyRestClientService class's callRestService method when the result is success.
     * It verifies that the method returns the expected greeting message.
     * 
     * The test sets up a mock server to handle the REST request and response.
     * It expects a GET request to "https://example.com/greet/details" and responds with a success status and a plain text response body of "hello".
     * 
     * The method then calls the callRestService method of the MyRestClientService class and stores the returned greeting message.
     * 
     * Finally, it asserts that the greeting message is equal to the expected value of "hello".
     * 
     * @throws Exception if an error occurs during the test
     */
    @Test
	void getVehicleDetailsWhenResultIsSuccessShouldReturnDetails() {
		this.server.expect(requestTo("https://example.com/greet/details"))
			.andRespond(withSuccess("hello", MediaType.TEXT_PLAIN));
		String greeting = this.service.callRestService();
		assertThat(greeting).isEqualTo("hello");
	}

}

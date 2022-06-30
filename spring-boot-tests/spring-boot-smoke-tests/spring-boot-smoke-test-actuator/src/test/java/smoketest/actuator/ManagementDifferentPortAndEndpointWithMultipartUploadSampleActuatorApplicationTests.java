/*
 * Copyright 2012-2022 the original author or authors.
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

package smoketest.actuator;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for separate management and main service ports with Actuator's MVC
 * {@link RestControllerEndpoint rest controller endpoints} and multipart uploads.
 *
 * @author Guirong Hu
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {
		ManagementDifferentPortAndEndpointWithMultipartUploadSampleActuatorApplicationTests.SecurityConfiguration.class,
		SampleActuatorApplication.class },
		properties = { "management.endpoints.web.exposure.include=*", "management.server.port=0" })
class ManagementDifferentPortAndEndpointWithMultipartUploadSampleActuatorApplicationTests {

	@LocalManagementPort
	private int managementPort;

	@Test
	void endpointShouldBeSupportMultipartUpload() {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

		MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();
		requestBody.add("file", new ClassPathResource("test-file.txt"));

		HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(requestBody, requestHeaders);

		ResponseEntity<String> responseEntity = new TestRestTemplate("user", "password").postForEntity(
				"http://localhost:" + this.managementPort + "/actuator/rest/upload", requestEntity, String.class);
		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(responseEntity.getBody()).isEqualTo("this is a test file");
	}

	@Configuration(proxyBeanMethods = false)
	static class SecurityConfiguration {

		@Bean
		SecurityFilterChain configure(HttpSecurity http) throws Exception {
			http.csrf().disable();
			return http.build();
		}

	}

}

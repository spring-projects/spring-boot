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

package smoketest.jersey;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.http.server.LocalTestWebServer;
import org.springframework.context.ApplicationContext;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the starter with Jackson 3 explicitly selected.
 *
 * @author Kristoffer Larsen Hopland
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "spring.jersey.preferred-json-mapper=jackson")
class JerseyJackson3ApplicationTests extends AbstractJerseyApplicationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	@Override
	void actuatorStatus() {
		String uri = LocalTestWebServer.obtain(this.applicationContext).uri("/actuator/health");
		assertThat(RestClient.create().get().uri(uri).retrieve().body(String.class)).contains("\"status\":\"UP\"",
				"\"groups\":[\"liveness\",\"readiness\"]");
		assertThat(this.applicationContext.containsBean("jacksonResourceConfigCustomizer")).isTrue();
		assertThat(this.applicationContext.containsBean("jackson2ResourceConfigCustomizer")).isFalse();
	}

}

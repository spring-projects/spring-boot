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

package smoketest.tomcat.ssl;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.AbstractConfigurableWebServerFactory;
import org.springframework.boot.web.server.Ssl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.json.JsonContent;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class SampleTomcatSslApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private AbstractConfigurableWebServerFactory webServerFactory;

	@Test
	void testSsl() {
		Ssl ssl = this.webServerFactory.getSsl();
		assertThat(ssl).isNotNull();
		assertThat(ssl.isEnabled()).isTrue();
	}

	@Test
	void testHome() {
		ResponseEntity<String> entity = this.restTemplate.getForEntity("/", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).isEqualTo("Hello, world");
	}

	@Test
	void testSslInfo() {
		ResponseEntity<String> entity = this.restTemplate.getForEntity("/actuator/info", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		String body = entity.getBody();
		assertThat(body).isNotNull();
		JsonContent json = new JsonContent(body);
		assertThat(json).extractingPath("ssl.bundles[0].name").isEqualTo("ssldemo");
		assertThat(json).extractingPath("ssl.bundles[0].certificateChains[0].alias")
			.isEqualTo("spring-boot-ssl-sample");
		assertThat(json).extractingPath("ssl.bundles[0].certificateChains[0].certificates[0].issuer")
			.isEqualTo("CN=localhost,OU=Unknown,O=Unknown,L=Unknown,ST=Unknown,C=Unknown");
		assertThat(json).extractingPath("ssl.bundles[0].certificateChains[0].certificates[0].subject")
			.isEqualTo("CN=localhost,OU=Unknown,O=Unknown,L=Unknown,ST=Unknown,C=Unknown");
		assertThat(json).extractingPath("ssl.bundles[0].certificateChains[0].certificates[0].validity.status")
			.isEqualTo("EXPIRED");
		assertThat(json).extractingPath("ssl.bundles[0].certificateChains[0].certificates[0].validity.message")
			.asString()
			.startsWith("Not valid after ");
	}

	@Test
	void testSslHealth() {
		ResponseEntity<String> entity = this.restTemplate.getForEntity("/actuator/health", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
		String body = entity.getBody();
		assertThat(body).isNotNull();
		JsonContent json = new JsonContent(body);
		assertThat(json).extractingPath("status").isEqualTo("OUT_OF_SERVICE");
		assertThat(json).extractingPath("components.ssl.status").isEqualTo("OUT_OF_SERVICE");
		assertThat(json).extractingPath("components.ssl.details.invalidChains[0].alias")
			.isEqualTo("spring-boot-ssl-sample");
		assertThat(json).extractingPath("components.ssl.details.invalidChains[0].certificates[0].issuer")
			.isEqualTo("CN=localhost,OU=Unknown,O=Unknown,L=Unknown,ST=Unknown,C=Unknown");
		assertThat(json).extractingPath("components.ssl.details.invalidChains[0].certificates[0].subject")
			.isEqualTo("CN=localhost,OU=Unknown,O=Unknown,L=Unknown,ST=Unknown,C=Unknown");
		assertThat(json).extractingPath("components.ssl.details.invalidChains[0].certificates[0].validity.status")
			.isEqualTo("EXPIRED");
		assertThat(json).extractingPath("components.ssl.details.invalidChains[0].certificates[0].validity.message")
			.asString()
			.startsWith("Not valid after ");
	}

}

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
import org.springframework.boot.resttestclient.TrustAllTlsRequestFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.server.AbstractConfigurableWebServerFactory;
import org.springframework.boot.web.server.Ssl;
import org.springframework.http.HttpStatus;
import org.springframework.test.json.JsonContent;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class SampleTomcatSslApplicationTests {

	@LocalServerPort
	private int port;

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
		restTestClient().get().uri("/").exchangeSuccessfully().expectBody(String.class).isEqualTo("Hello, world");
	}

	@Test
	void testSslInfo() {
		EntityExchangeResult<String> result = restTestClient().get()
			.uri("/actuator/info")
			.exchange()
			.returnResult(String.class);
		assertThat(result.getStatus()).isEqualTo(HttpStatus.OK);
		String body = result.getResponseBody();
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
		EntityExchangeResult<String> result = restTestClient().get()
			.uri("/actuator/health")
			.exchange()
			.returnResult(String.class);
		assertThat(result.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
		String body = result.getResponseBody();
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

	private RestTestClient restTestClient() {
		return RestTestClient.bindToServer(TrustAllTlsRequestFactory.create())
			.baseUrl("https://localhost:" + this.port)
			.build();
	}

}

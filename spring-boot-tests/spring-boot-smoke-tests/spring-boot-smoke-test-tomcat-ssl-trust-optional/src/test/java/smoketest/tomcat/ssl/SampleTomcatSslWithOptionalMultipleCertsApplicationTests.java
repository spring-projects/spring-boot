/*
 * Copyright 2012-2024 the original author or authors.
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.io.ApplicationResourceLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = {
		"spring.ssl.bundle.pem.server.keystore.certificates=classpath:certs/server.crt",
		"spring.ssl.bundle.pem.server.keystore.private-key=classpath:certs/server.key",
		"spring.ssl.bundle.pem.server.keystore.private-key-password=123456",
		"spring.ssl.bundle.pem.server.truststore.certificates[0]=optional:${user.dir}/build/resources/main/newca.crt",
		"spring.ssl.bundle.pem.server.truststore.certificates[1]=optional:classpath:certs/ca_cert.crt",
		"spring.ssl.bundle.pem.server.reload-on-update=true",
		"spring.ssl.bundle.pem.rest.keystore.certificates=classpath:certs/client.crt",
		"spring.ssl.bundle.pem.rest.keystore.private-key=classpath:certs/client.key",
		"spring.ssl.bundle.pem.rest.truststore.certificates=classpath:certs/ca.crt",
		"server.ssl.client-auth=need",
		"server.port=8443",
		"server.ssl.bundle=server",
		"spring.ssl.bundle.watch.file.quiet-period=5ms"})
@ContextConfiguration(initializers = { SampleTomcatSslWithOptionalMultipleCertsApplicationTests.Initializer.class})
class SampleTomcatSslWithOptionalMultipleCertsApplicationTests {

	private static final String OPTIONAL_PREFIX = "optional:";

	@LocalServerPort
	int port;

	@Autowired
	private RestTemplate restTemplate;

	@Value("${spring.ssl.bundle.pem.server.truststore.certificates[0]}")
	private String targetFile;

	public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			try {
				Resource resource = new ApplicationResourceLoader().getResource("classpath:newca.crt");
				if (resource.exists()) {
					resource.getFile().delete();
				}
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Test
	void testHome() {
		final String URL = "https://localhost:%s/".formatted(this.port);

		ResourceAccessException exception = assertThrows(ResourceAccessException.class, () -> this.restTemplate.getForEntity(URL, String.class));
		String expectedMessage = "Received fatal alert: bad_certificate";
		assertThat(exception.getCause().getMessage()).isEqualTo(expectedMessage);

		copyCaFileFromResourceToExpectTruststoreCertificateProperty();

		Awaitility.await().atMost(Duration.ofMinutes(1)).ignoreExceptions().untilAsserted(() -> {
			ResponseEntity<String> entity = this.restTemplate.getForEntity(URL, String.class);
			assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(entity.getBody()).isEqualTo("Hello, world");
		});
	}

	private void copyCaFileFromResourceToExpectTruststoreCertificateProperty() {
		Path sourcePath = Paths.get("src/main/resources/certs", "ca.crt");
		if (this.targetFile != null) {
			Path pathFile = Paths.get(this.targetFile.replace(OPTIONAL_PREFIX, ""));
			try {
				try (InputStream inputStream = Files.newInputStream(sourcePath)) {
					Files.copy(inputStream, pathFile);
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}

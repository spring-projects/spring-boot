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

package smoketest.webflux.ssl;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import javax.net.ssl.TrustManagerFactory;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import reactor.netty.http.client.HttpClient;

import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.fail;

/**
 * Integration tests of Spring Boot's SSL server configured to use a PKCS#11 keystore
 * (HSM).
 *
 * @author Cyril Dangerville
 */
@Testcontainers(disabledWithoutDocker = true)
class EmbeddedNettySslServerWithPkcs11KeystoreTests {

	@Test
	void launchWithPkcs11KeystoreProvider() {
		/*
		 * We are going to use the server certificate of the keypair generated in the
		 * PKCS#11 HSM inside the container, as trusted certificate for the SSL
		 * connection, to make sure that the Netty SSL server is actually using this
		 * certificate and the associated keypair in the HSM. The certificate is extracted
		 * to /server-cert.pem by the keytool command run inside the container at startup
		 * (see src/test/resources/docker-entrypoint.sh).
		 */
		final File serverCertDestinationFile = new File("build/tmp/test/server-cert.pem");
		final ToStringConsumer consumer = new ToStringConsumer().withRemoveAnsiCodes(false);
		try (SpringBootJarTestContainer container = new SpringBootJarTestContainer()) {
			container.withLogConsumer(consumer);
			container.start();
			assertThat(consumer.toUtf8String().contains("Netty started"));

			// HTTPS connection test
			container.copyFileFromContainer("/server-cert.pem", serverCertDestinationFile.getAbsolutePath());
			final KeyStore truststore = KeyStore.getInstance(KeyStore.getDefaultType());
			truststore.load(null, null);
			final CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
			final Certificate cert;
			try (FileInputStream input = new FileInputStream(serverCertDestinationFile)) {
				cert = certFactory.generateCertificate(input);
			}
			truststore.setCertificateEntry("server", cert);
			TrustManagerFactory trustManagerFactory = TrustManagerFactory
					.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			trustManagerFactory.init(truststore);
			final SslContext sslContext = SslContextBuilder.forClient().trustManager(trustManagerFactory).build();
			final HttpClient httpClient = HttpClient.create().secure((sslSpec) -> sslSpec.sslContext(sslContext));
			final WebClient httpsClient = WebClient.builder()
					.clientConnector(new ReactorClientHttpConnector(httpClient)).build();
			assertThatNoException()
					.isThrownBy(() -> httpsClient.get().uri("https://localhost:" + container.getFirstMappedPort() + "/")
							.retrieve().toEntity(String.class).block());
			return;
		}
		catch (Throwable ex) {
			ex.printStackTrace();
		}

		fail("Container failed to start or SSL test failed. Startup logs: " + consumer.toUtf8String());
	}

	private static final class SpringBootJarTestContainer extends GenericContainer<SpringBootJarTestContainer> {

		private SpringBootJarTestContainer() {
			super(new ImageFromDockerfile("spring-boot-smoke-test-webflux-ssl/ssl-server-with-pkcs11-keystore")
					.withFileFromFile("Dockerfile",
							new File("src/test/resources/ssl-server-with-pkcs11-keystore/Dockerfile")));
			withCopyFileToContainer(MountableFile.forHostPath(new File(
					"build/spring-boot-starter-webflux-tests-app/build/libs/spring-boot-starter-webflux-tests-app.jar")
							.getAbsolutePath()),
					"/app.jar");
			final String startupScript = "docker-entrypoint.sh";
			withCopyFileToContainer(
					MountableFile.forHostPath("src/test/resources/ssl-server-with-pkcs11-keystore/" + startupScript),
					"/" + startupScript);
			withCommand("/bin/bash", "-c", "chown root:root *.sh && chown root:root *.jar && chmod +x " + startupScript
					+ " && ./" + startupScript);
			withExposedPorts(8443);
			waitingFor(Wait.forListeningPort());
		}

	}

}

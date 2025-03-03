/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.tomcat;

import java.util.Collections;

import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.testsupport.classpath.resources.WithPackageResources;
import org.springframework.boot.testsupport.ssl.MockPkcs11Security;
import org.springframework.boot.testsupport.ssl.MockPkcs11SecurityProvider;
import org.springframework.boot.testsupport.system.OutputCaptureExtension;
import org.springframework.boot.testsupport.web.servlet.DirtiesUrlFactories;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServerSslBundle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for {@link SslConnectorCustomizer}
 *
 * @author Brian Clozel
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @author Cyril Dangerville
 */
@ExtendWith(OutputCaptureExtension.class)
@DirtiesUrlFactories
@MockPkcs11Security
class SslConnectorCustomizerTests {

	private final Log logger = LogFactory.getLog(SslConnectorCustomizerTests.class);

	private Tomcat tomcat;

	@BeforeEach
	void setup() {
		this.tomcat = new Tomcat();
		Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
		connector.setPort(0);
		this.tomcat.setConnector(connector);
	}

	@AfterEach
	void stop() throws Exception {
		System.clearProperty("javax.net.ssl.trustStorePassword");
		this.tomcat.stop();
	}

	@Test
	@WithPackageResources("test.jks")
	void sslCiphersConfiguration() throws Exception {
		Ssl ssl = new Ssl();
		ssl.setKeyStore("classpath:test.jks");
		ssl.setKeyStorePassword("secret");
		ssl.setCiphers(new String[] { "ALPHA", "BRAVO", "CHARLIE" });
		Connector connector = this.tomcat.getConnector();
		SslConnectorCustomizer customizer = new SslConnectorCustomizer(this.logger, connector, ssl.getClientAuth());
		customizer.customize(WebServerSslBundle.get(ssl), Collections.emptyMap());
		this.tomcat.start();
		SSLHostConfig[] sslHostConfigs = connector.getProtocolHandler().findSslHostConfigs();
		assertThat(sslHostConfigs[0].getCiphers()).isEqualTo("ALPHA:BRAVO:CHARLIE");
	}

	@Test
	@WithPackageResources("test.jks")
	void sslEnabledMultipleProtocolsConfiguration() throws Exception {
		Ssl ssl = new Ssl();
		ssl.setKeyPassword("password");
		ssl.setKeyStore("classpath:test.jks");
		ssl.setEnabledProtocols(new String[] { "TLSv1.1", "TLSv1.2" });
		ssl.setCiphers(new String[] { "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256", "BRAVO" });
		Connector connector = this.tomcat.getConnector();
		SslConnectorCustomizer customizer = new SslConnectorCustomizer(this.logger, connector, ssl.getClientAuth());
		customizer.customize(WebServerSslBundle.get(ssl), Collections.emptyMap());
		this.tomcat.start();
		SSLHostConfig sslHostConfig = connector.getProtocolHandler().findSslHostConfigs()[0];
		assertThat(sslHostConfig.getSslProtocol()).isEqualTo("TLS");
		assertThat(sslHostConfig.getEnabledProtocols()).containsExactlyInAnyOrder("TLSv1.1", "TLSv1.2");
	}

	@Test
	@WithPackageResources("test.jks")
	void sslEnabledProtocolsConfiguration() throws Exception {
		Ssl ssl = new Ssl();
		ssl.setKeyPassword("password");
		ssl.setKeyStore("classpath:test.jks");
		ssl.setEnabledProtocols(new String[] { "TLSv1.2" });
		ssl.setCiphers(new String[] { "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256", "BRAVO" });
		Connector connector = this.tomcat.getConnector();
		SslConnectorCustomizer customizer = new SslConnectorCustomizer(this.logger, connector, ssl.getClientAuth());
		customizer.customize(WebServerSslBundle.get(ssl), Collections.emptyMap());
		this.tomcat.start();
		SSLHostConfig sslHostConfig = connector.getProtocolHandler().findSslHostConfigs()[0];
		assertThat(sslHostConfig.getSslProtocol()).isEqualTo("TLS");
		assertThat(sslHostConfig.getEnabledProtocols()).containsExactly("TLSv1.2");
	}

	@Test
	void customizeWhenSslIsEnabledWithNoKeyStoreAndNotPkcs11ThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> {
			SslConnectorCustomizer customizer = new SslConnectorCustomizer(this.logger, this.tomcat.getConnector(),
					Ssl.ClientAuth.NONE);
			customizer.customize(WebServerSslBundle.get(new Ssl()), Collections.emptyMap());
		}).withMessageContaining("SSL is enabled but no trust material is configured");
	}

	@Test
	@WithPackageResources("test.jks")
	void customizeWhenSslIsEnabledWithPkcs11AndKeyStoreThrowsException() {
		Ssl ssl = new Ssl();
		ssl.setKeyStoreType("PKCS11");
		ssl.setKeyStoreProvider(MockPkcs11SecurityProvider.NAME);
		ssl.setKeyStore("classpath:test.jks");
		ssl.setKeyPassword("password");
		assertThatIllegalStateException().isThrownBy(() -> {
			SslConnectorCustomizer customizer = new SslConnectorCustomizer(this.logger, this.tomcat.getConnector(),
					ssl.getClientAuth());
			customizer.customize(WebServerSslBundle.get(ssl), Collections.emptyMap());
		}).withMessageContaining("must be empty or null for PKCS11 hardware key stores");
	}

	@Test
	void customizeWhenSslIsEnabledWithPkcs11AndKeyStoreProvider() {
		Ssl ssl = new Ssl();
		ssl.setKeyStoreType("PKCS11");
		ssl.setKeyStoreProvider(MockPkcs11SecurityProvider.NAME);
		ssl.setKeyStorePassword("1234");
		SslConnectorCustomizer customizer = new SslConnectorCustomizer(this.logger, this.tomcat.getConnector(),
				ssl.getClientAuth());
		assertThatNoException()
			.isThrownBy(() -> customizer.customize(WebServerSslBundle.get(ssl), Collections.emptyMap()));
	}

}

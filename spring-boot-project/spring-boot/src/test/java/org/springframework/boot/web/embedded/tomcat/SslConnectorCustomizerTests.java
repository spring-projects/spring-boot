/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.web.embedded.tomcat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import org.apache.catalina.LifecycleState;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.TomcatURLStreamHandlerFactory;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.testsupport.system.CapturedOutput;
import org.springframework.boot.testsupport.system.OutputCaptureExtension;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.SslStoreProvider;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SslConnectorCustomizer}
 *
 * @author Brian Clozel
 */
@ExtendWith(OutputCaptureExtension.class)
class SslConnectorCustomizerTests {

	private Tomcat tomcat;

	private Connector connector;

	@BeforeEach
	void setup() {
		this.tomcat = new Tomcat();
		this.connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
		this.connector.setPort(0);
		this.tomcat.setConnector(this.connector);
	}

	@AfterEach
	void stop() throws Exception {
		System.clearProperty("javax.net.ssl.trustStorePassword");
		ReflectionTestUtils.setField(TomcatURLStreamHandlerFactory.class, "instance", null);
		ReflectionTestUtils.setField(URL.class, "factory", null);
		this.tomcat.stop();
	}

	@Test
	void sslCiphersConfiguration() throws Exception {
		Ssl ssl = new Ssl();
		ssl.setKeyStore("test.jks");
		ssl.setKeyStorePassword("secret");
		ssl.setCiphers(new String[] { "ALPHA", "BRAVO", "CHARLIE" });
		SslConnectorCustomizer customizer = new SslConnectorCustomizer(ssl, null);
		Connector connector = this.tomcat.getConnector();
		customizer.customize(connector);
		this.tomcat.start();
		SSLHostConfig[] sslHostConfigs = connector.getProtocolHandler().findSslHostConfigs();
		assertThat(sslHostConfigs[0].getCiphers()).isEqualTo("ALPHA:BRAVO:CHARLIE");
	}

	@Test
	void sslEnabledMultipleProtocolsConfiguration() throws Exception {
		Ssl ssl = new Ssl();
		ssl.setKeyPassword("password");
		ssl.setKeyStore("src/test/resources/test.jks");
		ssl.setEnabledProtocols(new String[] { "TLSv1.1", "TLSv1.2" });
		ssl.setCiphers(new String[] { "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256", "BRAVO" });
		SslConnectorCustomizer customizer = new SslConnectorCustomizer(ssl, null);
		Connector connector = this.tomcat.getConnector();
		customizer.customize(connector);
		this.tomcat.start();
		SSLHostConfig sslHostConfig = connector.getProtocolHandler().findSslHostConfigs()[0];
		assertThat(sslHostConfig.getSslProtocol()).isEqualTo("TLS");
		assertThat(sslHostConfig.getEnabledProtocols()).containsExactlyInAnyOrder("TLSv1.1", "TLSv1.2");
	}

	@Test
	void sslEnabledProtocolsConfiguration() throws Exception {
		Ssl ssl = new Ssl();
		ssl.setKeyPassword("password");
		ssl.setKeyStore("src/test/resources/test.jks");
		ssl.setEnabledProtocols(new String[] { "TLSv1.2" });
		ssl.setCiphers(new String[] { "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256", "BRAVO" });
		SslConnectorCustomizer customizer = new SslConnectorCustomizer(ssl, null);
		Connector connector = this.tomcat.getConnector();
		customizer.customize(connector);
		this.tomcat.start();
		SSLHostConfig sslHostConfig = connector.getProtocolHandler().findSslHostConfigs()[0];
		assertThat(sslHostConfig.getSslProtocol()).isEqualTo("TLS");
		assertThat(sslHostConfig.getEnabledProtocols()).containsExactly("TLSv1.2");
	}

	@Test
	void customizeWhenSslStoreProviderProvidesOnlyKeyStoreShouldUseDefaultTruststore() throws Exception {
		Ssl ssl = new Ssl();
		ssl.setKeyPassword("password");
		ssl.setTrustStore("src/test/resources/test.jks");
		SslStoreProvider sslStoreProvider = mock(SslStoreProvider.class);
		given(sslStoreProvider.getKeyStore()).willReturn(loadStore());
		SslConnectorCustomizer customizer = new SslConnectorCustomizer(ssl, sslStoreProvider);
		Connector connector = this.tomcat.getConnector();
		customizer.customize(connector);
		this.tomcat.start();
		SSLHostConfig sslHostConfig = connector.getProtocolHandler().findSslHostConfigs()[0];
		SSLHostConfig sslHostConfigWithDefaults = new SSLHostConfig();
		assertThat(sslHostConfig.getTruststoreFile()).isEqualTo(sslHostConfigWithDefaults.getTruststoreFile());
		assertThat(sslHostConfig.getCertificateKeystoreFile())
				.isEqualTo(SslStoreProviderUrlStreamHandlerFactory.KEY_STORE_URL);
	}

	@Test
	void customizeWhenSslStoreProviderProvidesOnlyTrustStoreShouldUseDefaultKeystore() throws Exception {
		Ssl ssl = new Ssl();
		ssl.setKeyPassword("password");
		ssl.setKeyStore("src/test/resources/test.jks");
		SslStoreProvider sslStoreProvider = mock(SslStoreProvider.class);
		given(sslStoreProvider.getTrustStore()).willReturn(loadStore());
		SslConnectorCustomizer customizer = new SslConnectorCustomizer(ssl, sslStoreProvider);
		Connector connector = this.tomcat.getConnector();
		customizer.customize(connector);
		this.tomcat.start();
		SSLHostConfig sslHostConfig = connector.getProtocolHandler().findSslHostConfigs()[0];
		sslHostConfig.getCertificates(true);
		SSLHostConfig sslHostConfigWithDefaults = new SSLHostConfig();
		sslHostConfigWithDefaults.getCertificates(true);
		assertThat(sslHostConfig.getTruststoreFile())
				.isEqualTo(SslStoreProviderUrlStreamHandlerFactory.TRUST_STORE_URL);
		assertThat(sslHostConfig.getCertificateKeystoreFile())
				.contains(sslHostConfigWithDefaults.getCertificateKeystoreFile());
	}

	@Test
	void customizeWhenSslStoreProviderPresentShouldIgnorePasswordFromSsl(CapturedOutput output) throws Exception {
		System.setProperty("javax.net.ssl.trustStorePassword", "trustStoreSecret");
		Ssl ssl = new Ssl();
		ssl.setKeyPassword("password");
		ssl.setKeyStorePassword("secret");
		SslStoreProvider sslStoreProvider = mock(SslStoreProvider.class);
		given(sslStoreProvider.getTrustStore()).willReturn(loadStore());
		given(sslStoreProvider.getKeyStore()).willReturn(loadStore());
		SslConnectorCustomizer customizer = new SslConnectorCustomizer(ssl, sslStoreProvider);
		Connector connector = this.tomcat.getConnector();
		customizer.customize(connector);
		this.tomcat.start();
		assertThat(connector.getState()).isEqualTo(LifecycleState.STARTED);
		assertThat(output).doesNotContain("Password verification failed");
	}

	@Test
	void customizeWhenSslIsEnabledWithNoKeyStoreThrowsWebServerException() {
		assertThatExceptionOfType(WebServerException.class)
				.isThrownBy(() -> new SslConnectorCustomizer(new Ssl(), null).customize(this.tomcat.getConnector()))
				.withMessageContaining("Could not load key store 'null'");
	}

	private KeyStore loadStore() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
		KeyStore keyStore = KeyStore.getInstance("JKS");
		Resource resource = new ClassPathResource("test.jks");
		try (InputStream inputStream = resource.getInputStream()) {
			keyStore.load(inputStream, "secret".toCharArray());
			return keyStore;
		}
	}

}

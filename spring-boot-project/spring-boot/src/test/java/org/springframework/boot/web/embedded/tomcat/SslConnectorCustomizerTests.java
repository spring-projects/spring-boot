/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.web.embedded.tomcat;

import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.web.server.Ssl;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SslConnectorCustomizer}
 *
 * @author Brian Clozel
 */
public class SslConnectorCustomizerTests {

	private Tomcat tomcat;

	private Connector connector;

	@Before
	public void setup() {
		this.tomcat = new Tomcat();
		this.connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
		this.connector.setPort(0);
		this.tomcat.setConnector(this.connector);
	}

	@After
	public void stop() throws Exception {
		this.tomcat.stop();
	}

	@Test
	public void sslCiphersConfiguration() throws Exception {
		Ssl ssl = new Ssl();
		ssl.setKeyStore("test.jks");
		ssl.setKeyStorePassword("secret");
		ssl.setCiphers(new String[] { "ALPHA", "BRAVO", "CHARLIE" });
		SslConnectorCustomizer customizer = new SslConnectorCustomizer(ssl, null);
		Connector connector = this.tomcat.getConnector();
		customizer.customize(connector);
		this.tomcat.start();
		SSLHostConfig[] sslHostConfigs = connector.getProtocolHandler()
				.findSslHostConfigs();
		assertThat(sslHostConfigs[0].getCiphers()).isEqualTo("ALPHA:BRAVO:CHARLIE");
	}

	@Test
	public void sslEnabledMultipleProtocolsConfiguration() throws Exception {
		Ssl ssl = new Ssl();
		ssl.setKeyPassword("password");
		ssl.setKeyStore("src/test/resources/test.jks");
		ssl.setEnabledProtocols(new String[] { "TLSv1.1", "TLSv1.2" });
		ssl.setCiphers(new String[] { "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256", "BRAVO" });
		SslConnectorCustomizer customizer = new SslConnectorCustomizer(ssl, null);
		Connector connector = this.tomcat.getConnector();
		customizer.customize(connector);
		this.tomcat.start();
		SSLHostConfig sslHostConfig = connector.getProtocolHandler()
				.findSslHostConfigs()[0];
		assertThat(sslHostConfig.getSslProtocol()).isEqualTo("TLS");
		assertThat(sslHostConfig.getEnabledProtocols())
				.containsExactlyInAnyOrder("TLSv1.1", "TLSv1.2");
	}

	@Test
	public void sslEnabledProtocolsConfiguration() throws Exception {
		Ssl ssl = new Ssl();
		ssl.setKeyPassword("password");
		ssl.setKeyStore("src/test/resources/test.jks");
		ssl.setEnabledProtocols(new String[] { "TLSv1.2" });
		ssl.setCiphers(new String[] { "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256", "BRAVO" });
		SslConnectorCustomizer customizer = new SslConnectorCustomizer(ssl, null);
		Connector connector = this.tomcat.getConnector();
		customizer.customize(connector);
		this.tomcat.start();
		SSLHostConfig sslHostConfig = connector.getProtocolHandler()
				.findSslHostConfigs()[0];
		assertThat(sslHostConfig.getSslProtocol()).isEqualTo("TLS");
		assertThat(sslHostConfig.getEnabledProtocols()).containsExactly("TLSv1.2");
	}

}

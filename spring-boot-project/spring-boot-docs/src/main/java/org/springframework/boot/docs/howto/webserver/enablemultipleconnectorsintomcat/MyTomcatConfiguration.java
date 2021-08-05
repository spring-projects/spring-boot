/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.docs.howto.webserver.enablemultipleconnectorsintomcat;

import java.io.IOException;
import java.net.URL;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ResourceUtils;

@Configuration(proxyBeanMethods = false)
public class MyTomcatConfiguration {

	@Bean
	public WebServerFactoryCustomizer<TomcatServletWebServerFactory> sslConnectorCustomizer() {
		return (tomcat) -> tomcat.addAdditionalTomcatConnectors(createSslConnector());
	}

	private Connector createSslConnector() {
		Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
		Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
		try {
			URL keystore = ResourceUtils.getURL("keystore");
			URL truststore = ResourceUtils.getURL("truststore");
			connector.setScheme("https");
			connector.setSecure(true);
			connector.setPort(8443);
			protocol.setSSLEnabled(true);
			protocol.setKeystoreFile(keystore.toString());
			protocol.setKeystorePass("changeit");
			protocol.setTruststoreFile(truststore.toString());
			protocol.setTruststorePass("changeit");
			protocol.setKeyAlias("apitester");
			return connector;
		}
		catch (IOException ex) {
			throw new IllegalStateException("Fail to create ssl connector", ex);
		}
	}

}

/*
 * Copyright 2012-2013 the original author or authors.
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

package sample.tomcat;

import java.io.File;
import java.io.IOException;

import org.apache.catalina.connector.Connector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

/**
 * Sample Application to show tomcat running 2 connectors
 * 
 * @author Brock Mills
 * 
 */
@Configuration
@EnableAutoConfiguration
@ComponentScan
public class SampleTomcatTwoConnectorsApplication {

	private static Log logger = LogFactory.getLog(SampleTomcatTwoConnectorsApplication.class);

	public static void main(String[] args) throws Exception {
		SpringApplication.run(SampleTomcatTwoConnectorsApplication.class, args);
	}

	/**
	 * creates a ssl connector and adds it to the factory
	 * @return
	 */
	@Bean
	public EmbeddedServletContainerFactory servletContainer() {

		final int sslPort = 8443;
		final String keystoreFile = "keystore";
		final String keystorePass = "changeit";
		final String truststoreFile = "keystore";
		final String truststorePass = "changeit";
		final String privateKeyAlias = "apitester";

		TomcatEmbeddedServletContainerFactory tomcat = new TomcatEmbeddedServletContainerFactory();

		Connector sslConnector = new Connector(
				"org.apache.coyote.http11.Http11NioProtocol");
		try {
			File keystore = new ClassPathResource(keystoreFile).getFile();
			File truststore = new ClassPathResource(truststoreFile).getFile();
			sslConnector.setScheme("https");
			sslConnector.setSecure(true);
			sslConnector.setPort(sslPort);
			Http11NioProtocol protocol = (Http11NioProtocol) sslConnector.getProtocolHandler();
			protocol.setSSLEnabled(true);
			protocol.setKeystoreFile(keystore.getAbsolutePath());
			protocol.setKeystorePass(keystorePass);
			protocol.setTruststoreFile(truststore.getAbsolutePath());
			protocol.setTruststorePass(truststorePass);
			protocol.setKeyAlias(privateKeyAlias);

		} catch (IOException e) {
			throw new RuntimeException("cant access keystore: [" + keystoreFile + "] or truststore: [" + truststoreFile + "]", e);
		}

		tomcat.addAdditionalTomcatConnectors(sslConnector);

		return tomcat;
	}
}

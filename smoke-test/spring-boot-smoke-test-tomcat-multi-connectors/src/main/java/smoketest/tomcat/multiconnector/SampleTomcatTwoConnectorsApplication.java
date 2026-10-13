/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.tomcat.multiconnector;

import org.apache.catalina.connector.Connector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.servlet.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;

/**
 * Sample Application to show Tomcat running two connectors.
 *
 * @author Brock Mills
 * @author Andy Wilkinson
 */
@SpringBootApplication
public class SampleTomcatTwoConnectorsApplication {

	@Bean
	public ServletWebServerFactory servletContainer() {
		TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
		tomcat.addAdditionalConnectors(createStandardConnector());
		return tomcat;
	}

	private Connector createStandardConnector() {
		Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
		connector.setPort(0);
		return connector;
	}

	public static void main(String[] args) {
		SpringApplication.run(SampleTomcatTwoConnectorsApplication.class, args);
	}

}

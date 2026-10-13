/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.web.reactive.reactiveserver.customizing.programmatic;

import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.reactive.ConfigurableReactiveWebServerFactory;
import org.springframework.stereotype.Component;

@Component
public class MyWebServerFactoryCustomizer implements WebServerFactoryCustomizer<ConfigurableReactiveWebServerFactory> {

	@Override
	public void customize(ConfigurableReactiveWebServerFactory server) {
		server.setPort(9000);
	}

}

/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.web.reactive.reactiveserver.customizing.programmatic;

import java.time.Duration;

import org.springframework.boot.reactor.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

@Component
public class MyNettyWebServerFactoryCustomizer implements WebServerFactoryCustomizer<NettyReactiveWebServerFactory> {

	@Override
	public void customize(NettyReactiveWebServerFactory factory) {
		factory.addServerCustomizers((server) -> server.idleTimeout(Duration.ofSeconds(20)));
	}

}

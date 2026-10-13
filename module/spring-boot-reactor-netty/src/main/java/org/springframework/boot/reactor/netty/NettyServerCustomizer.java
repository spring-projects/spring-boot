/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.reactor.netty;

import java.util.function.Function;

import reactor.netty.http.server.HttpServer;

/**
 * Mapping function that can be used to customize a Reactor Netty server instance.
 *
 * @author Brian Clozel
 * @since 4.0.0
 * @see NettyReactiveWebServerFactory
 */
@FunctionalInterface
public interface NettyServerCustomizer extends Function<HttpServer, HttpServer> {

}

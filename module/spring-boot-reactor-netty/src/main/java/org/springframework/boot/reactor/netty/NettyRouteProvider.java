/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.reactor.netty;

import java.util.function.Function;

import reactor.netty.http.server.HttpServerRoutes;

/**
 * Function that can add new routes to an {@link HttpServerRoutes} instance.
 *
 * @author Brian Clozel
 * @since 4.0.0
 * @see NettyReactiveWebServerFactory
 */
@FunctionalInterface
public interface NettyRouteProvider extends Function<HttpServerRoutes, HttpServerRoutes> {

}

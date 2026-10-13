/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.reactive;

import org.springframework.boot.web.server.ConfigurableWebServerFactory;

/**
 * Configurable {@link ReactiveWebServerFactory}.
 *
 * @author Brian Clozel
 * @since 4.0.0
 */
public interface ConfigurableReactiveWebServerFactory extends ConfigurableWebServerFactory, ReactiveWebServerFactory {

}

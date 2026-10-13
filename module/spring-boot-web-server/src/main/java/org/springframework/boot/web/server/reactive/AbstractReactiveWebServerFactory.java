/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.reactive;

import org.springframework.boot.web.server.AbstractConfigurableWebServerFactory;

/**
 * Abstract base class for {@link ReactiveWebServerFactory} implementations.
 *
 * @author Brian Clozel
 * @since 4.0.0
 */
public abstract class AbstractReactiveWebServerFactory extends AbstractConfigurableWebServerFactory
		implements ConfigurableReactiveWebServerFactory {

	public AbstractReactiveWebServerFactory() {
	}

	public AbstractReactiveWebServerFactory(int port) {
		super(port);
	}

}

/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.context.reactive;

import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;

/**
 * {@link Environment} implementation to be used by {@code Reactive}-based web
 * applications. All web-related (reactive-based) {@code ApplicationContext} classes
 * initialize an instance by default.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
public class StandardReactiveWebEnvironment extends StandardEnvironment implements ConfigurableReactiveWebEnvironment {

	public StandardReactiveWebEnvironment() {
		super();
	}

	protected StandardReactiveWebEnvironment(MutablePropertySources propertySources) {
		super(propertySources);
	}

}

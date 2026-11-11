/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.context.reactive;

import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Specialization of {@link ConfigurableEnvironment} for reactive application contexts.
 *
 * @author Phillip Webb
 * @since 2.0.0
 * @see ConfigurableReactiveWebApplicationContext#getEnvironment()
 */
public interface ConfigurableReactiveWebEnvironment extends ConfigurableEnvironment {

}

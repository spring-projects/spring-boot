/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.bootstrap;

/**
 * A {@link BootstrapContext} that also provides configuration methods through the
 * {@link BootstrapRegistry} interface.
 *
 * @author Phillip Webb
 * @since 4.0.0
 * @see BootstrapRegistry
 * @see BootstrapContext
 * @see DefaultBootstrapContext
 */
public interface ConfigurableBootstrapContext extends BootstrapRegistry, BootstrapContext {

}

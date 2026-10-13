/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.bootstrap;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * {@link ApplicationEvent} published by a {@link BootstrapContext} when it's closed.
 *
 * @author Phillip Webb
 * @since 4.0.0
 * @see BootstrapRegistry#addCloseListener(org.springframework.context.ApplicationListener)
 */
public class BootstrapContextClosedEvent extends ApplicationEvent {

	private final ConfigurableApplicationContext applicationContext;

	BootstrapContextClosedEvent(BootstrapContext source, ConfigurableApplicationContext applicationContext) {
		super(source);
		this.applicationContext = applicationContext;
	}

	/**
	 * Return the {@link BootstrapContext} that was closed.
	 * @return the bootstrap context
	 */
	public BootstrapContext getBootstrapContext() {
		return (BootstrapContext) this.source;
	}

	/**
	 * Return the prepared application context.
	 * @return the application context
	 */
	public ConfigurableApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

}

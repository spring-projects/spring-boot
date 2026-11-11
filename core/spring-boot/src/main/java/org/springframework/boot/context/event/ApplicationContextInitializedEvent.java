/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.event;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Event published when a {@link SpringApplication} is starting up and the
 * {@link ApplicationContext} is prepared and ApplicationContextInitializers have been
 * called but before any bean definitions are loaded.
 *
 * @author Artsiom Yudovin
 * @since 2.1.0
 */
@SuppressWarnings("serial")
public class ApplicationContextInitializedEvent extends SpringApplicationEvent {

	private final ConfigurableApplicationContext context;

	/**
	 * Create a new {@link ApplicationContextInitializedEvent} instance.
	 * @param application the current application
	 * @param args the arguments the application is running with
	 * @param context the context that has been initialized
	 */
	public ApplicationContextInitializedEvent(SpringApplication application, String[] args,
			ConfigurableApplicationContext context) {
		super(application, args);
		this.context = context;
	}

	/**
	 * Return the application context.
	 * @return the context
	 */
	public ConfigurableApplicationContext getApplicationContext() {
		return this.context;
	}

}

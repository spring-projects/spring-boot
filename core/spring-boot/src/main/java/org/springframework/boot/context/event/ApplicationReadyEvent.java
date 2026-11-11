/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.event;

import java.time.Duration;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Event published as late as conceivably possible to indicate that the application is
 * ready to service requests. The source of the event is the {@link SpringApplication}
 * itself, but beware of modifying its internal state since all initialization steps will
 * have been completed by then.
 *
 * @author Stephane Nicoll
 * @author Chris Bono
 * @since 1.3.0
 * @see ApplicationFailedEvent
 */
@SuppressWarnings("serial")
public class ApplicationReadyEvent extends SpringApplicationEvent {

	private final ConfigurableApplicationContext context;

	private final @Nullable Duration timeTaken;

	/**
	 * Create a new {@link ApplicationReadyEvent} instance.
	 * @param application the current application
	 * @param args the arguments the application is running with
	 * @param context the context that was being created
	 * @param timeTaken the time taken to get the application ready to service requests
	 * @since 2.6.0
	 */
	public ApplicationReadyEvent(SpringApplication application, String[] args, ConfigurableApplicationContext context,
			@Nullable Duration timeTaken) {
		super(application, args);
		this.context = context;
		this.timeTaken = timeTaken;
	}

	/**
	 * Return the application context.
	 * @return the context
	 */
	public ConfigurableApplicationContext getApplicationContext() {
		return this.context;
	}

	/**
	 * Return the time taken for the application to be ready to service requests, or
	 * {@code null} if unknown.
	 * @return the time taken to be ready to service requests
	 * @since 2.6.0
	 */
	public @Nullable Duration getTimeTaken() {
		return this.timeTaken;
	}

}

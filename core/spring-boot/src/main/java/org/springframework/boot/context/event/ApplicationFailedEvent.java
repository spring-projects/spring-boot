/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.event;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Event published by a {@link SpringApplication} when it fails to start.
 *
 * @author Dave Syer
 * @since 1.0.0
 * @see ApplicationReadyEvent
 */
@SuppressWarnings("serial")
public class ApplicationFailedEvent extends SpringApplicationEvent {

	private final @Nullable ConfigurableApplicationContext context;

	private final Throwable exception;

	/**
	 * Create a new {@link ApplicationFailedEvent} instance.
	 * @param application the current application
	 * @param args the arguments the application was running with
	 * @param context the context that was being created (maybe null)
	 * @param exception the exception that caused the error
	 */
	public ApplicationFailedEvent(SpringApplication application, String[] args,
			@Nullable ConfigurableApplicationContext context, Throwable exception) {
		super(application, args);
		this.context = context;
		this.exception = exception;
	}

	/**
	 * Return the application context.
	 * @return the context or {@code null}
	 */
	public @Nullable ConfigurableApplicationContext getApplicationContext() {
		return this.context;
	}

	/**
	 * Return the exception that caused the failure.
	 * @return the exception
	 */
	public Throwable getException() {
		return this.exception;
	}

}

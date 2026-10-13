/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.contributor;

import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Base {@link HealthIndicator} implementations that encapsulates creation of
 * {@link Health} instance and error handling.
 * <p>
 * This implementation is only suitable if an {@link Exception} raised from
 * {@link #doHealthCheck(Health.Builder)} should create a {@link Status#DOWN} health
 * status.
 *
 * @author Christian Dupuis
 * @since 4.0.0
 */
public abstract class AbstractHealthIndicator implements HealthIndicator {

	private static final @Nullable String NO_MESSAGE = null;

	private static final String DEFAULT_MESSAGE = "Health check failed";

	private final Log logger = LogFactory.getLog(getClass());

	private final Function<Exception, @Nullable String> healthCheckFailedMessage;

	/**
	 * Create a new {@link AbstractHealthIndicator} instance with a default
	 * {@code healthCheckFailedMessage}.
	 */
	protected AbstractHealthIndicator() {
		this(NO_MESSAGE);
	}

	/**
	 * Create a new {@link AbstractHealthIndicator} instance with a specific message to
	 * log when the health check fails.
	 * @param healthCheckFailedMessage the message to log on health check failure
	 */
	protected AbstractHealthIndicator(@Nullable String healthCheckFailedMessage) {
		this.healthCheckFailedMessage = (ex) -> healthCheckFailedMessage;
	}

	/**
	 * Create a new {@link AbstractHealthIndicator} instance with a specific message to
	 * log when the health check fails.
	 * @param healthCheckFailedMessage the message to log on health check failure
	 */
	protected AbstractHealthIndicator(Function<Exception, @Nullable String> healthCheckFailedMessage) {
		Assert.notNull(healthCheckFailedMessage, "'healthCheckFailedMessage' must not be null");
		this.healthCheckFailedMessage = healthCheckFailedMessage;
	}

	@Override
	public final Health health() {
		Health.Builder builder = new Health.Builder();
		try {
			doHealthCheck(builder);
		}
		catch (Exception ex) {
			builder.down(ex);
		}
		logExceptionIfPresent(builder.getException());
		return builder.build();
	}

	private void logExceptionIfPresent(@Nullable Throwable throwable) {
		if (throwable != null && this.logger.isWarnEnabled()) {
			String message = (throwable instanceof Exception ex) ? this.healthCheckFailedMessage.apply(ex) : null;
			this.logger.warn(StringUtils.hasText(message) ? message : DEFAULT_MESSAGE, throwable);
		}
	}

	/**
	 * Actual health check logic.
	 * @param builder the {@link Health.Builder} to report health status and details
	 * @throws Exception any {@link Exception} that should create a {@link Status#DOWN}
	 * system status.
	 */
	protected abstract void doHealthCheck(Health.Builder builder) throws Exception;

}

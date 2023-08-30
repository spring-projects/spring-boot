/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.health;

import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Base {@link ReactiveHealthIndicator} implementations that encapsulates creation of
 * {@link Health} instance and error handling.
 *
 * @author Stephane Nicoll
 * @author Nikolay Rybak
 * @author Moritz Halbritter
 * @since 2.0.0
 */
public abstract class AbstractReactiveHealthIndicator implements ReactiveHealthIndicator {

	private static final String NO_MESSAGE = null;

	private static final String DEFAULT_MESSAGE = "Health check failed";

	private final Log logger = LogFactory.getLog(getClass());

	private final Function<Throwable, String> healthCheckFailedMessage;

	/**
	 * Create a new {@link AbstractReactiveHealthIndicator} instance with a default
	 * {@code healthCheckFailedMessage}.
	 * @since 2.1.7
	 */
	protected AbstractReactiveHealthIndicator() {
		this(NO_MESSAGE);
	}

	/**
	 * Create a new {@link AbstractReactiveHealthIndicator} instance with a specific
	 * message to log when the health check fails.
	 * @param healthCheckFailedMessage the message to log on health check failure
	 * @since 2.1.7
	 */
	protected AbstractReactiveHealthIndicator(String healthCheckFailedMessage) {
		this.healthCheckFailedMessage = (ex) -> healthCheckFailedMessage;
	}

	/**
	 * Create a new {@link AbstractReactiveHealthIndicator} instance with a specific
	 * message to log when the health check fails.
	 * @param healthCheckFailedMessage the message to log on health check failure
	 * @since 2.1.7
	 */
	protected AbstractReactiveHealthIndicator(Function<Throwable, String> healthCheckFailedMessage) {
		Assert.notNull(healthCheckFailedMessage, "HealthCheckFailedMessage must not be null");
		this.healthCheckFailedMessage = healthCheckFailedMessage;
	}

	@Override
	public final Mono<Health> health() {
		try {
			Health.Builder builder = new Health.Builder();
			Mono<Health> result = doHealthCheck(builder).onErrorResume(this::handleFailure);
			return result.doOnNext((health) -> logExceptionIfPresent(builder.getException()));
		}
		catch (Exception ex) {
			return handleFailure(ex);
		}
	}

	private void logExceptionIfPresent(Throwable ex) {
		if (ex != null && this.logger.isWarnEnabled()) {
			String message = (ex instanceof Exception) ? this.healthCheckFailedMessage.apply(ex) : null;
			this.logger.warn(StringUtils.hasText(message) ? message : DEFAULT_MESSAGE, ex);
		}
	}

	private Mono<Health> handleFailure(Throwable ex) {
		logExceptionIfPresent(ex);
		return Mono.just(new Health.Builder().down(ex).build());
	}

	/**
	 * Actual health check logic. If an error occurs in the pipeline, it will be handled
	 * automatically.
	 * @param builder the {@link Health.Builder} to report health status and details
	 * @return a {@link Mono} that provides the {@link Health}
	 */
	protected abstract Mono<Health> doHealthCheck(Health.Builder builder);

}

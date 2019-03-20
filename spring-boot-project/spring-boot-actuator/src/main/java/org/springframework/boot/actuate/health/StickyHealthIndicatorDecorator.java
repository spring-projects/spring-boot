package org.springframework.boot.actuate.health;

import org.springframework.util.Assert;

/**
 * Decorator for Health Indicator that suppresses any status except UP if
 * actual status was UP at least once.
 *
 * @author Vladislav Fefelov
 */
public class StickyHealthIndicatorDecorator implements HealthIndicator {

	private static final String ORIGINAL_STATUS_KEY = "originalStatus";

	private static final String ORIGINAL_DETAILS_KEY = "originalDetails";

	private final HealthIndicator delegate;

	private volatile boolean wasUp = false;

	public StickyHealthIndicatorDecorator(HealthIndicator healthIndicator) {
		Assert.notNull(healthIndicator, "HealthIndicator cannot be null");
		this.delegate = healthIndicator;
	}

	@Override
	public Health health() {
		final boolean previouslyWasUp = wasUp;

		final Health actualHealth = delegate.health();

		if (actualHealth.getStatus() == Status.UP) {
			if (!previouslyWasUp) {
				wasUp = true;
			}

			return actualHealth;
		}

		if (previouslyWasUp) {
			return Health.up()
				.withDetail(ORIGINAL_STATUS_KEY, actualHealth.getStatus())
				.withDetail(ORIGINAL_DETAILS_KEY, actualHealth.getDetails())
				.build();
		}

		return actualHealth;
	}

}

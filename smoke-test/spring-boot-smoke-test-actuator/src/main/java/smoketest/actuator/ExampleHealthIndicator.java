/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.actuator;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class ExampleHealthIndicator implements HealthIndicator {

	@Override
	public Health health() {
		return Health.up().withDetail("counter", 42).build();
	}

}

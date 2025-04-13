package org.springframework.boot.actuate.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class CustomHealthIndicator implements HealthIndicator {

	@Override
	public Health health() {
		// Simulating a check for a custom service. You can modify this logic based on your needs.
		boolean isHealthy = checkCustomServiceHealth();

		if (isHealthy) {
			return Health.up().withDetail("Custom Service", "Service is healthy").build();
		} else {
			return Health.down().withDetail("Custom Service", "Service is down").build();
		}
	}

	private boolean checkCustomServiceHealth() {
		// For this demo, we are returning true to simulate that the service is healthy.
		// You can replace this with actual logic like checking a database, API, etc.
		return true;
	}
}

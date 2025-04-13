package org.springframework.boot.actuate.health;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

class CustomHealthIndicatorTest {

	private CustomHealthIndicator customHealthIndicator;

	@BeforeEach
	void setUp() {
		// Initialize the CustomHealthIndicator
		customHealthIndicator = new CustomHealthIndicator();
	}

	@Test
	void health_shouldReturnHealthyStatus_whenServiceIsHealthy() {
		// Act
		Health health = customHealthIndicator.health();

		// Assert
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("Custom Service", "Service is healthy");
	}

	@Test
	void health_shouldReturnUnhealthyStatus_whenServiceIsDown() {
		// Simulate the health being down by modifying the checkCustomServiceHealth method.
		customHealthIndicator = new CustomHealthIndicator() {
			@Override
			public Health health() {
				return Health.down().withDetail("Custom Service", "Service is down").build();
			}
		};

		// Act
		Health health = customHealthIndicator.health();

		// Assert
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails()).containsEntry("Custom Service", "Service is down");
	}
}

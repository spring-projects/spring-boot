/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.grpc.server.autoconfigure.health;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.grpc.server.health.StatusAggregator;
import org.springframework.boot.grpc.server.health.StatusMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AutoConfiguredHealthCheckedGrpcComponent}.
 *
 * @author Phillip Webb
 */
@ExtendWith(MockitoExtension.class)
class AutoConfiguredHealthCheckedGrpcComponentTests {

	@Mock
	private StatusAggregator statusAggregator;

	@Mock
	private StatusMapper statusMapper;

	@Test
	void isMemberWhenMembershipMatchesAcceptsTrue() {
		AutoConfiguredHealthCheckedGrpcComponent component = new AutoConfiguredHealthCheckedGrpcComponent(
				(name) -> name.startsWith("a"), this.statusAggregator, this.statusMapper);
		assertThat(component.isMember("albert")).isTrue();
		assertThat(component.isMember("arnold")).isTrue();
	}

	@Test
	void isMemberWhenMembershipRejectsReturnsFalse() {
		AutoConfiguredHealthCheckedGrpcComponent component = new AutoConfiguredHealthCheckedGrpcComponent(
				(name) -> name.startsWith("a"), this.statusAggregator, this.statusMapper);
		assertThat(component.isMember("bert")).isFalse();
		assertThat(component.isMember("ernie")).isFalse();
	}

	@Test
	void getStatusAggregatorReturnsStatusAggregator() {
		AutoConfiguredHealthCheckedGrpcComponent component = new AutoConfiguredHealthCheckedGrpcComponent(
				(name) -> true, this.statusAggregator, this.statusMapper);
		assertThat(component.getStatusAggregator()).isSameAs(this.statusAggregator);
	}

	@Test
	void getStatusMapperReturnsStatusMapper() {
		AutoConfiguredHealthCheckedGrpcComponent component = new AutoConfiguredHealthCheckedGrpcComponent(
				(name) -> true, this.statusAggregator, this.statusMapper);
		assertThat(component.getStatusMapper()).isSameAs(this.statusMapper);
	}

}

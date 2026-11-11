/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.observation.autoconfigure;

import java.util.List;

import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationHandler.FirstMatchingCompositeObservationHandler;
import io.micrometer.observation.ObservationRegistry.ObservationConfig;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ObservationHandlerGroup}.
 *
 * @author Phillip Webb
 */
class ObservationHandlerGroupTests {

	@Test
	void isMemberWhenHandlerIsInstanceOfHandlerTypeReturnsTrue() {
		ObservationHandlerGroup group = ObservationHandlerGroup.of(TestObservationHandler.class);
		assertThat(group.isMember(mock(TestObservationHandler.class))).isTrue();
		assertThat(group.isMember(mock(TestObservationHandlerSubclass.class))).isTrue();
	}

	@Test
	void isMemberWhenHandlerIsNotInstanceOfHandlerTypeReturnsFalse() {
		ObservationHandlerGroup group = ObservationHandlerGroup.of(TestObservationHandler.class);
		assertThat(group.isMember(mock(ObservationHandler.class))).isFalse();
		assertThat(group.isMember(mock(OtherObservationHandler.class))).isFalse();
	}

	@Test
	void registerMembersRegistersUsingFirstMatchingCompositeObservationHandler() {
		ObservationHandlerGroup group = ObservationHandlerGroup.of(TestObservationHandler.class);
		TestObservationHandler handler1 = mock(TestObservationHandler.class);
		TestObservationHandler handler2 = mock(TestObservationHandler.class);
		ObservationConfig config = mock(ObservationConfig.class);
		group.registerMembers(config, List.of(handler1, handler2));
		ArgumentCaptor<ObservationHandler<?>> registeredHandler = ArgumentCaptor.captor();
		then(config).should().observationHandler(registeredHandler.capture());
		assertThat(registeredHandler.getValue()).isInstanceOf(FirstMatchingCompositeObservationHandler.class)
			.extracting("handlers")
			.asInstanceOf(InstanceOfAssertFactories.LIST)
			.containsExactly(handler1, handler2);
	}

	@Test
	void compareToReturnsZero() {
		ObservationHandlerGroup group1 = ObservationHandlerGroup.of(TestObservationHandler.class);
		ObservationHandlerGroup group2 = ObservationHandlerGroup.of(TestObservationHandler.class);
		assertThat(group1.compareTo(group1)).isZero();
		assertThat(group1.compareTo(group2)).isZero();
		assertThat(group2.compareTo(group1)).isZero();
	}

	@Test
	void ofCreatesHandlerGroup() {
		ObservationHandlerGroup group = ObservationHandlerGroup.of(TestObservationHandler.class);
		assertThat(group.handlerType()).isEqualTo(TestObservationHandler.class);
	}

	interface TestObservationHandler extends ObservationHandler<Context> {

	}

	interface TestObservationHandlerSubclass extends TestObservationHandler {

	}

	interface OtherObservationHandler extends ObservationHandler<Context> {

	}

}

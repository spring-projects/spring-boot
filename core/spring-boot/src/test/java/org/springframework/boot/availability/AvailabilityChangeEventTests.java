/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.availability;

import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.ResolvableType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AvailabilityChangeEvent}.
 *
 * @author Phillip Webb
 */
class AvailabilityChangeEventTests {

	private final Object source = new Object();

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenStateIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new AvailabilityChangeEvent<>(this.source, null))
			.withMessage("Payload must not be null");
	}

	@Test
	void getStateReturnsState() {
		LivenessState state = LivenessState.CORRECT;
		AvailabilityChangeEvent<LivenessState> event = new AvailabilityChangeEvent<>(this.source, state);
		assertThat(event.getState()).isEqualTo(state);
	}

	@Test
	void getResolvableType() {
		LivenessState state = LivenessState.CORRECT;
		AvailabilityChangeEvent<LivenessState> event = new AvailabilityChangeEvent<>(this.source, state);
		ResolvableType type = event.getResolvableType();
		assertThat(type.resolve()).isEqualTo(AvailabilityChangeEvent.class);
		assertThat(type.resolveGeneric()).isEqualTo(LivenessState.class);
	}

	@Test
	void getResolvableTypeWhenSubclassedEnum() {
		SubClassedEnum state = SubClassedEnum.TWO;
		AvailabilityChangeEvent<SubClassedEnum> event = new AvailabilityChangeEvent<>(this.source, state);
		ResolvableType type = event.getResolvableType();
		assertThat(type.resolve()).isEqualTo(AvailabilityChangeEvent.class);
		assertThat(type.resolveGeneric()).isEqualTo(SubClassedEnum.class);
	}

	@Test
	void publishPublishesEvent() {
		ApplicationContext context = mock(ApplicationContext.class);
		AvailabilityState state = LivenessState.CORRECT;
		AvailabilityChangeEvent.publish(context, state);
		then(context).should()
			.publishEvent(assertArg((event) -> assertThat(event).isInstanceOfSatisfying(AvailabilityChangeEvent.class,
					(castedEvent) -> {
						assertThat(castedEvent.getSource()).isEqualTo(context);
						assertThat(castedEvent.getState()).isEqualTo(state);
					})));
	}

	@Test
	void publishEvenToContextConsidersGenericType() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
		AvailabilityChangeEvent.publish(context, LivenessState.CORRECT);
		AvailabilityChangeEvent.publish(context, ReadinessState.ACCEPTING_TRAFFIC);
	}

	enum SubClassedEnum implements AvailabilityState {

		ONE {

			@Override
			String getDescription() {
				return "I have been overridden";
			}

		},

		TWO {

			@Override
			String getDescription() {
				return "I have also been overridden";
			}

		};

		abstract String getDescription();

	}

	@Configuration
	static class Config {

		@EventListener
		void onLivenessAvailabilityChange(AvailabilityChangeEvent<LivenessState> event) {
			assertThat(event.getState()).isInstanceOf(LivenessState.class).isEqualTo(LivenessState.CORRECT);
		}

	}

}

/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.convert;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

import org.jspecify.annotations.Nullable;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.TypeDescriptor;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Create a mock {@link TypeDescriptor} with optional {@link DurationUnit @DurationUnit}
 * and {@link DurationFormat @DurationFormat} annotations.
 *
 * @author Phillip Webb
 */
public final class MockDurationTypeDescriptor {

	private MockDurationTypeDescriptor() {
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static TypeDescriptor get(@Nullable ChronoUnit unit, @Nullable DurationStyle style) {
		TypeDescriptor descriptor = mock(TypeDescriptor.class);
		if (unit != null) {
			DurationUnit unitAnnotation = AnnotationUtils.synthesizeAnnotation(Collections.singletonMap("value", unit),
					DurationUnit.class, null);
			given(descriptor.getAnnotation(DurationUnit.class)).willReturn(unitAnnotation);
		}
		if (style != null) {
			DurationFormat formatAnnotation = AnnotationUtils
				.synthesizeAnnotation(Collections.singletonMap("value", style), DurationFormat.class, null);
			given(descriptor.getAnnotation(DurationFormat.class)).willReturn(formatAnnotation);
		}
		given(descriptor.getType()).willReturn((Class) Duration.class);
		given(descriptor.getObjectType()).willReturn((Class) Duration.class);
		return descriptor;
	}

}

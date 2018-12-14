/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.convert;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.TypeDescriptor;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Create a mock {@link TypeDescriptor} with optional {@link DurationUnit} and
 * {@link DurationFormat} annotations.
 *
 * @author Phillip Webb
 */
public final class MockDurationTypeDescriptor {

	private MockDurationTypeDescriptor() {
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static TypeDescriptor get(ChronoUnit unit, DurationStyle style) {
		TypeDescriptor descriptor = mock(TypeDescriptor.class);
		if (unit != null) {
			DurationUnit unitAnnotation = AnnotationUtils.synthesizeAnnotation(
					Collections.singletonMap("value", unit), DurationUnit.class, null);
			given(descriptor.getAnnotation(DurationUnit.class))
					.willReturn(unitAnnotation);
		}
		if (style != null) {
			DurationFormat formatAnnotation = AnnotationUtils.synthesizeAnnotation(
					Collections.singletonMap("value", style), DurationFormat.class, null);
			given(descriptor.getAnnotation(DurationFormat.class))
					.willReturn(formatAnnotation);
		}
		given(descriptor.getType()).willReturn((Class) Duration.class);
		given(descriptor.getObjectType()).willReturn((Class) Duration.class);
		return descriptor;
	}

}

/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.convert;

import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.TypeDescriptor;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Create a mock {@link TypeDescriptor} with optional {@link PeriodUnit @PeriodUnit} and
 * {@link PeriodFormat @PeriodFormat} annotations.
 *
 * @author Eddú Meléndez
 * @author Edson Chávez
 */
public final class MockPeriodTypeDescriptor {

	private MockPeriodTypeDescriptor() {
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static TypeDescriptor get(ChronoUnit unit, PeriodStyle style) {
		TypeDescriptor descriptor = mock(TypeDescriptor.class);
		if (unit != null) {
			PeriodUnit unitAnnotation = AnnotationUtils.synthesizeAnnotation(Collections.singletonMap("value", unit),
					PeriodUnit.class, null);
			given(descriptor.getAnnotation(PeriodUnit.class)).willReturn(unitAnnotation);
		}
		if (style != null) {
			PeriodFormat formatAnnotation = AnnotationUtils
					.synthesizeAnnotation(Collections.singletonMap("value", style), PeriodFormat.class, null);
			given(descriptor.getAnnotation(PeriodFormat.class)).willReturn(formatAnnotation);
		}
		given(descriptor.getType()).willReturn((Class) Period.class);
		given(descriptor.getObjectType()).willReturn((Class) Period.class);
		return descriptor;
	}

}

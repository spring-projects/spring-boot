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

import java.util.Collections;

import org.springframework.boot.DigitalAmount;
import org.springframework.boot.DigitalAmountStyle;
import org.springframework.boot.DigitalUnit;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.TypeDescriptor;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Create a mock {@link TypeDescriptor} with optional {@link DigitalAmountUnit} and
 * {@link DigitalAmountFormat} annotations.
 *
 * @author Dmytro Nosan
 */
public final class MockDigitalAmountTypeDescriptor {

	private MockDigitalAmountTypeDescriptor() {
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static TypeDescriptor get(DigitalUnit unit, DigitalAmountStyle style) {
		TypeDescriptor descriptor = mock(TypeDescriptor.class);
		if (unit != null) {
			DigitalAmountUnit unitAnnotation = AnnotationUtils.synthesizeAnnotation(
					Collections.singletonMap("value", unit), DigitalAmountUnit.class,
					null);
			given(descriptor.getAnnotation(DigitalAmountUnit.class))
					.willReturn(unitAnnotation);
		}
		if (style != null) {
			DigitalAmountFormat formatAnnotation = AnnotationUtils.synthesizeAnnotation(
					Collections.singletonMap("value", style), DigitalAmountFormat.class,
					null);
			given(descriptor.getAnnotation(DigitalAmountFormat.class))
					.willReturn(formatAnnotation);
		}
		given(descriptor.getType()).willReturn((Class) DigitalAmount.class);
		given(descriptor.getObjectType()).willReturn((Class) DigitalAmount.class);
		return descriptor;
	}

}

/*
 * Copyright 2012-2018 the original author or authors.
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

import java.util.Collections;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Create a mock {@link TypeDescriptor} with optional {@link DataSizeUnit} annotation.
 *
 * @author Stephane Nicoll
 */
public final class MockDataSizeTypeDescriptor {

	private MockDataSizeTypeDescriptor() {
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static TypeDescriptor get(DataUnit unit) {
		TypeDescriptor descriptor = mock(TypeDescriptor.class);
		if (unit != null) {
			DataSizeUnit unitAnnotation = AnnotationUtils.synthesizeAnnotation(
					Collections.singletonMap("value", unit), DataSizeUnit.class, null);
			given(descriptor.getAnnotation(DataSizeUnit.class))
					.willReturn(unitAnnotation);
		}
		given(descriptor.getType()).willReturn((Class) DataSize.class);
		given(descriptor.getObjectType()).willReturn((Class) DataSize.class);
		return descriptor;
	}

}

/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.convert;

import java.util.Collections;

import org.jspecify.annotations.Nullable;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Create a mock {@link TypeDescriptor} with optional {@link DataSizeUnit @DataSizeUnit}
 * annotation.
 *
 * @author Stephane Nicoll
 */
public final class MockDataSizeTypeDescriptor {

	private MockDataSizeTypeDescriptor() {
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static TypeDescriptor get(@Nullable DataUnit unit) {
		TypeDescriptor descriptor = mock(TypeDescriptor.class);
		if (unit != null) {
			DataSizeUnit unitAnnotation = AnnotationUtils.synthesizeAnnotation(Collections.singletonMap("value", unit),
					DataSizeUnit.class, null);
			given(descriptor.getAnnotation(DataSizeUnit.class)).willReturn(unitAnnotation);
		}
		given(descriptor.getType()).willReturn((Class) DataSize.class);
		given(descriptor.getObjectType()).willReturn((Class) DataSize.class);
		return descriptor;
	}

}

/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.context.properties.bind;

import java.lang.annotation.Annotation;
import java.util.List;

import org.junit.Test;

import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.TypeDescriptor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ResolvableTypeDescriptor}.
 *
 * @author Phillip Webb
 */
public class ResolvableTypeDescriptorTests {

	@Test
	public void forBindableShouldIncludeType() throws Exception {
		ResolvableType type = ResolvableType.forClassWithGenerics(List.class,
				String.class);
		Bindable<?> bindable = Bindable.of(type);
		TypeDescriptor descriptor = ResolvableTypeDescriptor.forBindable(bindable);
		assertThat(descriptor.getResolvableType()).isEqualTo(type);
	}

	@Test
	public void forBindableShouldIncludeAnnotations() throws Exception {
		Annotation annotation = AnnotationUtils.synthesizeAnnotation(Test.class);
		Bindable<?> bindable = Bindable.of(String.class).withAnnotations(annotation);
		TypeDescriptor descriptor = ResolvableTypeDescriptor.forBindable(bindable);
		assertThat(descriptor.getAnnotations()).containsExactly(annotation);
	}

	@Test
	public void forTypeShouldIncludeType() throws Exception {
		ResolvableType type = ResolvableType.forClassWithGenerics(List.class,
				String.class);
		TypeDescriptor descriptor = ResolvableTypeDescriptor.forType(type);
		assertThat(descriptor.getResolvableType()).isEqualTo(type);
	}

}

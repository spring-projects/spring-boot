/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.data.autoconfigure.metrics;

import java.lang.reflect.Method;

import io.micrometer.observation.annotation.Observed;
import org.junit.jupiter.api.Test;

import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ObservedAnnotations}.
 *
 * @author Kwonneung Lee
 */
class ObservedAnnotationsTests {

	@Test
	void getWhenNoneReturnsNull() {
		Method method = ReflectionUtils.findMethod(None.class, "handle");
		assertThat(method).isNotNull();
		assertThat(ObservedAnnotations.get(method, None.class)).isNull();
	}

	@Test
	void getWhenOnMethodReturnsMethodAnnotation() {
		Method method = ReflectionUtils.findMethod(OnMethod.class, "handle");
		assertThat(method).isNotNull();
		Observed observed = ObservedAnnotations.get(method, OnMethod.class);
		assertThat(observed).isNotNull();
		assertThat(observed.name()).isEqualTo("method.observed");
	}

	@Test
	void getWhenOnClassReturnsClassAnnotation() {
		Method method = ReflectionUtils.findMethod(OnClass.class, "handle");
		assertThat(method).isNotNull();
		Observed observed = ObservedAnnotations.get(method, OnClass.class);
		assertThat(observed).isNotNull();
		assertThat(observed.name()).isEqualTo("class.observed");
	}

	@Test
	void getWhenOnMethodAndClassReturnsMethodAnnotation() {
		Method method = ReflectionUtils.findMethod(OnMethodAndClass.class, "handle");
		assertThat(method).isNotNull();
		Observed observed = ObservedAnnotations.get(method, OnMethodAndClass.class);
		assertThat(observed).isNotNull();
		assertThat(observed.name()).isEqualTo("method.observed");
	}

	static class None {

		void handle() {
		}

	}

	@Observed(name = "class.observed")
	static class OnMethod {

		@Observed(name = "method.observed")
		void handle() {
		}

	}

	@Observed(name = "class.observed")
	static class OnClass {

		void handle() {
		}

	}

	@Observed(name = "class.observed")
	static class OnMethodAndClass {

		@Observed(name = "method.observed")
		void handle() {
		}

	}

}

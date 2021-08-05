/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.metrics.annotation;

import java.lang.reflect.Method;
import java.util.Set;

import io.micrometer.core.annotation.Timed;
import org.junit.jupiter.api.Test;

import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TimedAnnotations}.
 *
 * @author Phillip Webb
 */
class TimedAnnotationsTests {

	@Test
	void getWhenNoneReturnsEmptySet() {
		Object bean = new None();
		Method method = ReflectionUtils.findMethod(bean.getClass(), "handle");
		Set<Timed> annotations = TimedAnnotations.get(method, bean.getClass());
		assertThat(annotations).isEmpty();
	}

	@Test
	void getWhenOnMethodReturnsMethodAnnotations() {
		Object bean = new OnMethod();
		Method method = ReflectionUtils.findMethod(bean.getClass(), "handle");
		Set<Timed> annotations = TimedAnnotations.get(method, bean.getClass());
		assertThat(annotations).extracting(Timed::value).containsOnly("y", "z");
	}

	@Test
	void getWhenNonOnMethodReturnsBeanAnnotations() {
		Object bean = new OnBean();
		Method method = ReflectionUtils.findMethod(bean.getClass(), "handle");
		Set<Timed> annotations = TimedAnnotations.get(method, bean.getClass());
		assertThat(annotations).extracting(Timed::value).containsOnly("y", "z");
	}

	static class None {

		void handle() {
		}

	}

	@Timed("x")
	static class OnMethod {

		@Timed("y")
		@Timed("z")
		void handle() {
		}

	}

	@Timed("y")
	@Timed("z")
	static class OnBean {

		void handle() {
		}

	}

}

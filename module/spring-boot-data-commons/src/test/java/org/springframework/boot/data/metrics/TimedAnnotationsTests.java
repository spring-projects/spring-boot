/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.metrics;

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
		assertThat(method).isNotNull();
		Set<Timed> annotations = TimedAnnotations.get(method, bean.getClass());
		assertThat(annotations).isEmpty();
	}

	@Test
	void getWhenOnMethodReturnsMethodAnnotations() {
		Object bean = new OnMethod();
		Method method = ReflectionUtils.findMethod(bean.getClass(), "handle");
		assertThat(method).isNotNull();
		Set<Timed> annotations = TimedAnnotations.get(method, bean.getClass());
		assertThat(annotations).extracting(Timed::value).containsOnly("y", "z");
	}

	@Test
	void getWhenNonOnMethodReturnsBeanAnnotations() {
		Object bean = new OnBean();
		Method method = ReflectionUtils.findMethod(bean.getClass(), "handle");
		assertThat(method).isNotNull();
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

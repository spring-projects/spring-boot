/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.context.annotation;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link Configurations}.
 *
 * @author Phillip Webb
 */
class ConfigurationsTests {

	@Test
	void createWhenClassesIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new TestConfigurations((Collection<Class<?>>) null))
			.withMessageContaining("'classes' must not be null");
	}

	@Test
	@Deprecated(since = "3.4.0", forRemoval = true)
	void createShouldSortClassesUsingSortMethod() {
		TestDeprecatedSortedConfigurations configurations = new TestDeprecatedSortedConfigurations(
				Arrays.asList(OutputStream.class, InputStream.class));
		assertThat(configurations.getClasses()).containsExactly(InputStream.class, OutputStream.class);
	}

	@Test
	@Deprecated(since = "3.4.0", forRemoval = true)
	void getClassesShouldMergeByClassAndSortUsingSortMethod() {
		Configurations c1 = new TestDeprecatedSortedConfigurations(
				Arrays.asList(OutputStream.class, InputStream.class));
		Configurations c2 = new TestConfigurations(Collections.singletonList(Short.class));
		Configurations c3 = new TestDeprecatedSortedConfigurations(Arrays.asList(String.class, Integer.class));
		Configurations c4 = new TestConfigurations(Arrays.asList(Long.class, Byte.class));
		Class<?>[] classes = Configurations.getClasses(c1, c2, c3, c4);
		assertThat(classes).containsExactly(Short.class, Long.class, Byte.class, InputStream.class, Integer.class,
				OutputStream.class, String.class);
	}

	@Test
	void createShouldSortClasses() {
		TestConfigurations configurations = new TestConfigurations(Sorter.instance, OutputStream.class,
				InputStream.class);
		assertThat(configurations.getClasses()).containsExactly(InputStream.class, OutputStream.class);
	}

	@Test
	void getClassesShouldMergeByClassAndSort() {
		Configurations c1 = new TestSortedConfigurations(OutputStream.class, InputStream.class);
		Configurations c2 = new TestConfigurations(Short.class);
		Configurations c3 = new TestSortedConfigurations(String.class, Integer.class);
		Configurations c4 = new TestConfigurations(Long.class, Byte.class);
		Class<?>[] classes = Configurations.getClasses(c1, c2, c3, c4);
		assertThat(classes).containsExactly(Short.class, Long.class, Byte.class, InputStream.class, Integer.class,
				OutputStream.class, String.class);
	}

	@Test
	void getBeanNameWhenNoFunctionReturnsNull() {
		Configurations configurations = new TestConfigurations(Short.class);
		assertThat(configurations.getBeanName(Short.class)).isNull();
	}

	@Test
	void getBeanNameWhenFunctionReturnsBeanName() {
		Configurations configurations = new TestConfigurations(Sorter.instance, List.of(Short.class), Class::getName);
		assertThat(configurations.getBeanName(Short.class)).isEqualTo(Short.class.getName());
	}

	@Order(Ordered.HIGHEST_PRECEDENCE)
	static class TestConfigurations extends Configurations {

		TestConfigurations(Class<?>... classes) {
			this(Arrays.asList(classes));
		}

		TestConfigurations(UnaryOperator<Collection<Class<?>>> sorter, Class<?>... classes) {
			this(sorter, Arrays.asList(classes), null);
		}

		TestConfigurations(UnaryOperator<Collection<Class<?>>> sorter, Collection<Class<?>> classes,
				Function<Class<?>, String> beanNameGenerator) {
			super(sorter, classes, beanNameGenerator);
		}

		TestConfigurations(Collection<Class<?>> classes) {
			super(classes);
		}

		@Override
		protected Configurations merge(Set<Class<?>> mergedClasses) {
			return new TestConfigurations(mergedClasses);
		}

	}

	@Order(Ordered.LOWEST_PRECEDENCE)
	static class TestSortedConfigurations extends Configurations {

		protected TestSortedConfigurations(Class<?>... classes) {
			this(Arrays.asList(classes));
		}

		protected TestSortedConfigurations(Collection<Class<?>> classes) {
			super(Sorter.instance, classes, null);
		}

		@Override
		protected Configurations merge(Set<Class<?>> mergedClasses) {
			return new TestSortedConfigurations(mergedClasses);
		}

	}

	@Order(Ordered.LOWEST_PRECEDENCE)
	@SuppressWarnings("removal")
	static class TestDeprecatedSortedConfigurations extends Configurations {

		protected TestDeprecatedSortedConfigurations(Collection<Class<?>> classes) {
			super(classes);
		}

		@Override
		protected Collection<Class<?>> sort(Collection<Class<?>> classes) {
			return Sorter.instance.apply(classes);
		}

		@Override
		protected Configurations merge(Set<Class<?>> mergedClasses) {
			return new TestDeprecatedSortedConfigurations(mergedClasses);
		}

	}

	static class Sorter implements UnaryOperator<Collection<Class<?>>> {

		static final Sorter instance = new Sorter();

		@Override
		public Collection<Class<?>> apply(Collection<Class<?>> classes) {
			ArrayList<Class<?>> sorted = new ArrayList<>(classes);
			sorted.sort(Comparator.comparing(ClassUtils::getShortName));
			return sorted;

		}

	}

}

/*
 * Copyright 2012-2019 the original author or authors.
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
import java.util.Set;

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
		assertThatIllegalArgumentException().isThrownBy(() -> new TestConfigurations(null))
				.withMessageContaining("Classes must not be null");
	}

	@Test
	void createShouldSortClasses() {
		TestSortedConfigurations configurations = new TestSortedConfigurations(
				Arrays.asList(OutputStream.class, InputStream.class));
		assertThat(configurations.getClasses()).containsExactly(InputStream.class, OutputStream.class);
	}

	@Test
	void getClassesShouldMergeByClassAndSort() {
		Configurations c1 = new TestSortedConfigurations(Arrays.asList(OutputStream.class, InputStream.class));
		Configurations c2 = new TestConfigurations(Collections.singletonList(Short.class));
		Configurations c3 = new TestSortedConfigurations(Arrays.asList(String.class, Integer.class));
		Configurations c4 = new TestConfigurations(Arrays.asList(Long.class, Byte.class));
		Class<?>[] classes = Configurations.getClasses(c1, c2, c3, c4);
		assertThat(classes).containsExactly(Short.class, Long.class, Byte.class, InputStream.class, Integer.class,
				OutputStream.class, String.class);
	}

	@Order(Ordered.HIGHEST_PRECEDENCE)
	static class TestConfigurations extends Configurations {

		protected TestConfigurations(Collection<Class<?>> classes) {
			super(classes);
		}

		@Override
		protected Configurations merge(Set<Class<?>> mergedClasses) {
			return new TestConfigurations(mergedClasses);
		}

	}

	@Order(Ordered.LOWEST_PRECEDENCE)
	static class TestSortedConfigurations extends Configurations {

		protected TestSortedConfigurations(Collection<Class<?>> classes) {
			super(classes);
		}

		@Override
		protected Collection<Class<?>> sort(Collection<Class<?>> classes) {
			ArrayList<Class<?>> sorted = new ArrayList<>(classes);
			sorted.sort(Comparator.comparing(ClassUtils::getShortName));
			return sorted;
		}

		@Override
		protected Configurations merge(Set<Class<?>> mergedClasses) {
			return new TestSortedConfigurations(mergedClasses);
		}

	}

}

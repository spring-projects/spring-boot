/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link LazyInitializationExcludeFilter}.
 *
 * @author Phillip Webb
 */
class LazyInitializationExcludeFilterTests {

	@Test
	void forBeanTypesMatchesTypes() {
		LazyInitializationExcludeFilter filter = LazyInitializationExcludeFilter.forBeanTypes(CharSequence.class,
				Number.class);
		String beanName = "test";
		BeanDefinition beanDefinition = mock(BeanDefinition.class);
		assertThat(filter.isExcluded(beanName, beanDefinition, CharSequence.class)).isTrue();
		assertThat(filter.isExcluded(beanName, beanDefinition, String.class)).isTrue();
		assertThat(filter.isExcluded(beanName, beanDefinition, StringBuilder.class)).isTrue();
		assertThat(filter.isExcluded(beanName, beanDefinition, Number.class)).isTrue();
		assertThat(filter.isExcluded(beanName, beanDefinition, Long.class)).isTrue();
		assertThat(filter.isExcluded(beanName, beanDefinition, Boolean.class)).isFalse();
	}

}

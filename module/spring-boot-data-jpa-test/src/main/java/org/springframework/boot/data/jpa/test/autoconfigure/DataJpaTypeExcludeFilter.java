/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.jpa.test.autoconfigure;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.test.context.filter.annotation.StandardAnnotationCustomizableTypeExcludeFilter;

/**
 * {@link TypeExcludeFilter} for {@link DataJpaTest @DataJpaTest}.
 *
 * @author Phillip Webb
 */
class DataJpaTypeExcludeFilter extends StandardAnnotationCustomizableTypeExcludeFilter<DataJpaTest> {

	DataJpaTypeExcludeFilter(Class<?> testClass) {
		super(testClass);
	}

}

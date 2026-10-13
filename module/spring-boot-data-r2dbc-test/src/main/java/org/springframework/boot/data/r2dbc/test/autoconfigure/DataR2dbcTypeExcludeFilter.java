/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.r2dbc.test.autoconfigure;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.test.context.filter.annotation.StandardAnnotationCustomizableTypeExcludeFilter;

/**
 * {@link TypeExcludeFilter} for {@link DataR2dbcTest @DataR2dbcTest}.
 *
 * @author Mark Paluch
 */
class DataR2dbcTypeExcludeFilter extends StandardAnnotationCustomizableTypeExcludeFilter<DataR2dbcTest> {

	DataR2dbcTypeExcludeFilter(Class<?> testClass) {
		super(testClass);
	}

}

/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.mongodb.test.autoconfigure;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.test.context.filter.annotation.StandardAnnotationCustomizableTypeExcludeFilter;

/**
 * {@link TypeExcludeFilter} for {@link DataMongoTest @DataMongoTest}.
 *
 * @author Michael Simons
 */
class DataMongoTypeExcludeFilter extends StandardAnnotationCustomizableTypeExcludeFilter<DataMongoTest> {

	DataMongoTypeExcludeFilter(Class<?> testClass) {
		super(testClass);
	}

}

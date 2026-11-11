/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.redis.test.autoconfigure;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.test.context.filter.annotation.StandardAnnotationCustomizableTypeExcludeFilter;

/**
 * {@link TypeExcludeFilter} for {@link DataRedisTest @DataRedisTest}.
 *
 * @author Jayaram Pradhan
 */
class DataRedisTypeExcludeFilter extends StandardAnnotationCustomizableTypeExcludeFilter<DataRedisTest> {

	DataRedisTypeExcludeFilter(Class<?> testClass) {
		super(testClass);
	}

}

/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.test.autoconfigure;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.test.context.filter.annotation.StandardAnnotationCustomizableTypeExcludeFilter;

/**
 * {@link TypeExcludeFilter} for {@link JdbcTest @JdbcTest}.
 *
 * @author Stephane Nicoll
 */
class JdbcTypeExcludeFilter extends StandardAnnotationCustomizableTypeExcludeFilter<JdbcTest> {

	JdbcTypeExcludeFilter(Class<?> testClass) {
		super(testClass);
	}

}

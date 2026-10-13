/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jooq.test.autoconfigure;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.test.context.filter.annotation.StandardAnnotationCustomizableTypeExcludeFilter;

/**
 * {@link TypeExcludeFilter} for {@link JooqTest @JooqTest}.
 *
 * @author Michael Simons
 */
class JooqTypeExcludeFilter extends StandardAnnotationCustomizableTypeExcludeFilter<JooqTest> {

	JooqTypeExcludeFilter(Class<?> testClass) {
		super(testClass);
	}

}

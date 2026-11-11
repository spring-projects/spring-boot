/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.jdbc.test.autoconfigure;

import java.util.Collections;
import java.util.Set;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.test.context.filter.annotation.StandardAnnotationCustomizableTypeExcludeFilter;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;

/**
 * {@link TypeExcludeFilter} for {@link DataJdbcTest @DataJdbcTest}.
 *
 * @author Andy Wilkinson
 * @author Ravi Undupitiya
 */
class DataJdbcTypeExcludeFilter extends StandardAnnotationCustomizableTypeExcludeFilter<DataJdbcTest> {

	private static final Set<Class<?>> KNOWN_INCLUDES = Collections.singleton(AbstractJdbcConfiguration.class);

	DataJdbcTypeExcludeFilter(Class<?> testClass) {
		super(testClass);
	}

	@Override
	protected Set<Class<?>> getKnownIncludes() {
		return KNOWN_INCLUDES;
	}

}

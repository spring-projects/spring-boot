/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.data.jdbc;

import java.util.Collections;
import java.util.Set;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.test.autoconfigure.filter.StandardAnnotationCustomizableTypeExcludeFilter;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;

/**
 * {@link TypeExcludeFilter} for {@link DataJdbcTest @DataJdbcTest}.
 *
 * @author Andy Wilkinson
 * @author Ravi Undupitiya
 * @since 2.2.1
 */
public final class DataJdbcTypeExcludeFilter extends StandardAnnotationCustomizableTypeExcludeFilter<DataJdbcTest> {

	private static final Set<Class<?>> DEFAULT_INCLUDES = Collections.singleton(AbstractJdbcConfiguration.class);

	DataJdbcTypeExcludeFilter(Class<?> testClass) {
		super(testClass);
	}

	@Override
	protected Set<Class<?>> getDefaultIncludes() {
		return DEFAULT_INCLUDES;
	}

}

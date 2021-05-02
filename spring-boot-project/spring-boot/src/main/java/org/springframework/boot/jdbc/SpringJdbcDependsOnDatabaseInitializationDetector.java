/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.jdbc;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.sql.init.dependency.AbstractBeansOfTypeDependsOnDatabaseInitializationDetector;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitializationDetector;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

/**
 * {@link DependsOnDatabaseInitializationDetector} for Spring Framework's JDBC support.
 *
 * @author Andy Wilkinson
 */
class SpringJdbcDependsOnDatabaseInitializationDetector
		extends AbstractBeansOfTypeDependsOnDatabaseInitializationDetector {

	@Override
	protected Set<Class<?>> getDependsOnDatabaseInitializationBeanTypes() {
		return new HashSet<>(Arrays.asList(JdbcOperations.class, NamedParameterJdbcOperations.class));
	}

}

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

package org.springframework.boot.sql.init.dependency;

import java.util.Set;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * Detects beans that depend on database initialization. Implementations should be
 * registered in {@code META-INF/spring.factories} under the key
 * {@code org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitializationDetector}.
 *
 * @author Andy Wilkinson
 * @since 2.5.0
 */
public interface DependsOnDatabaseInitializationDetector {

	/**
	 * Detect beans defined in the given {@code beanFactory} that depend on database
	 * initialization. If no beans are detected, an empty set is returned.
	 * @param beanFactory bean factory to examine
	 * @return names of any beans that depend upon database initialization
	 */
	Set<String> detect(ConfigurableListableBeanFactory beanFactory);

}

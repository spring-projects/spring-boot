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

import java.util.Collections;
import java.util.Set;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * Base class for {@link DependsOnDatabaseInitializationDetector
 * DependsOnDatabaseInitializationDetectors} that detect by type beans that depend upon
 * database initialization.
 *
 * @author Andy Wilkinson
 * @since 2.5.0
 */
public abstract class AbstractBeansOfTypeDependsOnDatabaseInitializationDetector
		implements DependsOnDatabaseInitializationDetector {

	@Override
	public Set<String> detect(ConfigurableListableBeanFactory beanFactory) {
		try {
			Set<Class<?>> types = getDependsOnDatabaseInitializationBeanTypes();
			return new BeansOfTypeDetector(types).detect(beanFactory);
		}
		catch (Throwable ex) {
			return Collections.emptySet();
		}
	}

	/**
	 * Returns the bean types that should be detected as depending on database
	 * initialization.
	 * @return the database initialization dependent bean types
	 */
	protected abstract Set<Class<?>> getDependsOnDatabaseInitializationBeanTypes();

}

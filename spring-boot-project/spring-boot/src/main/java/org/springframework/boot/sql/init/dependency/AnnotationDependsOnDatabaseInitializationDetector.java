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

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * {@link DependsOnDatabaseInitializationDetector} that detects beans annotated with
 * {@link DependsOnDatabaseInitialization}.
 *
 * @author Andy Wilkinson
 */
class AnnotationDependsOnDatabaseInitializationDetector implements DependsOnDatabaseInitializationDetector {

	@Override
	public Set<String> detect(ConfigurableListableBeanFactory beanFactory) {
		Set<String> dependentBeans = new HashSet<>();
		for (String beanName : beanFactory.getBeanDefinitionNames()) {
			if (beanFactory.findAnnotationOnBean(beanName, DependsOnDatabaseInitialization.class) != null) {
				dependentBeans.add(beanName);
			}
		}
		return dependentBeans;
	}

}

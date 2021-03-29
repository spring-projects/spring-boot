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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;

/**
 * Helper class for detecting beans of particular types in a bean factory.
 *
 * @author Andy Wilkinson
 */
class BeansOfTypeDetector {

	private final Set<Class<?>> types;

	BeansOfTypeDetector(Set<Class<?>> types) {
		this.types = types;
	}

	Set<String> detect(ListableBeanFactory beanFactory) {
		Set<String> beanNames = new HashSet<>();
		for (Class<?> type : this.types) {
			try {
				String[] names = beanFactory.getBeanNamesForType(type, true, false);
				Arrays.stream(names).map(BeanFactoryUtils::transformedBeanName).forEach(beanNames::add);
			}
			catch (Throwable ex) {
				// Continue
			}
		}
		return beanNames;
	}

}

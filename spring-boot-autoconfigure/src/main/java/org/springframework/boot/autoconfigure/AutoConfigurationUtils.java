/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * Convenience class for storing base packages during component scan, for reference later
 * (e.g. by JPA entity scanner).
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
public abstract class AutoConfigurationUtils {

	private static final String BASE_PACKAGES_BEAN = AutoConfigurationUtils.class
			.getName() + ".basePackages";

	private static Set<String> EXCLUDED_PACKAGES;
	static {
		Set<String> exclude = new HashSet<String>();
		exclude.add("org.springframework.data.rest.webmvc");
		EXCLUDED_PACKAGES = Collections.unmodifiableSet(exclude);
	}

	@SuppressWarnings("unchecked")
	public static List<String> getBasePackages(BeanFactory beanFactory) {
		try {
			return beanFactory.getBean(BASE_PACKAGES_BEAN, List.class);
		}
		catch (NoSuchBeanDefinitionException ex) {
			return Collections.emptyList();
		}
	}

	public static void storeBasePackages(ConfigurableListableBeanFactory beanFactory,
			List<String> basePackages) {
		if (!beanFactory.containsBean(BASE_PACKAGES_BEAN)) {
			beanFactory.registerSingleton(BASE_PACKAGES_BEAN, new ArrayList<String>());
		}
		List<String> storePackages = getBasePackages(beanFactory);
		for (String basePackage : basePackages) {
			if (!EXCLUDED_PACKAGES.contains(basePackage)
					&& !storePackages.contains(basePackage)) {
				storePackages.add(basePackage);
			}
		}
	}
}

/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

/**
 * Selects configuration classes for the management context configuration. Entries are
 * loaded from {@code /META-INF/spring.factories} under the
 * {@code org.springframework.boot.actuate.autoconfigure.ManagementContextConfiguration}
 * key.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @see ManagementContextConfiguration
 */
@Order(Ordered.LOWEST_PRECEDENCE)
class ManagementContextConfigurationsImportSelector
		implements DeferredImportSelector, BeanClassLoaderAware {

	private ClassLoader classLoader;

	@Override
	public String[] selectImports(AnnotationMetadata metadata) {
		// Find all possible auto configuration classes, filtering duplicates
		List<String> names = loadFactoryNames();
		Set<Class<?>> classes = new LinkedHashSet<Class<?>>();
		for (String factoryName : names) {
			classes.add(ClassUtils.resolveClassName(factoryName, this.classLoader));
		}
		return getSortedClassNames(new ArrayList<Class<?>>(classes));
	}

	protected List<String> loadFactoryNames() {
		return SpringFactoriesLoader
				.loadFactoryNames(ManagementContextConfiguration.class, this.classLoader);
	}

	private String[] getSortedClassNames(List<Class<?>> classes) {
		AnnotationAwareOrderComparator.sort(classes);
		List<String> names = new ArrayList<String>();
		for (Class<?> sourceClass : classes) {
			names.add(sourceClass.getName());
		}
		return names.toArray(new String[names.size()]);
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

}

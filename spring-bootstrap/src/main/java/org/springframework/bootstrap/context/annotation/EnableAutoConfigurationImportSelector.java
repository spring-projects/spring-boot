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

package org.springframework.bootstrap.context.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.type.AnnotationMetadata;

/**
 * {@link DeferredImportSelector} to handle {@link EnableAutoConfiguration
 * auto-configuration}.
 * 
 * @author Phillip Webb
 * @see EnableAutoConfiguration
 */
@Order(Ordered.LOWEST_PRECEDENCE)
class EnableAutoConfigurationImportSelector implements DeferredImportSelector,
		BeanClassLoaderAware {

	private ClassLoader beanClassLoader;

	@Override
	public String[] selectImports(AnnotationMetadata metadata) {
		AnnotationAttributes attributes = AnnotationAttributes.fromMap(metadata
				.getAnnotationAttributes(EnableAutoConfiguration.class.getName(), true));
		List<String> factories = new ArrayList<String>(
				SpringFactoriesLoader.loadFactoryNames(EnableAutoConfiguration.class,
						this.beanClassLoader));
		factories.removeAll(Arrays.asList(attributes.getStringArray("exclude")));
		return factories.toArray(new String[factories.size()]);
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

}

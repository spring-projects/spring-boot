/*
 * Copyright 2012-2015 the original author or authors.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;

/**
 * {@link DeferredImportSelector} to handle {@link EnableAutoConfiguration
 * auto-configuration}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @see EnableAutoConfiguration
 */
@Order(Ordered.LOWEST_PRECEDENCE)
class EnableAutoConfigurationImportSelector implements DeferredImportSelector,
		BeanClassLoaderAware, ResourceLoaderAware, BeanFactoryAware {

	private ConfigurableListableBeanFactory beanFactory;

	private ClassLoader beanClassLoader;

	private ResourceLoader resourceLoader;

	@Override
	public String[] selectImports(AnnotationMetadata metadata) {
		try {
			AnnotationAttributes attributes = AnnotationAttributes.fromMap(metadata
					.getAnnotationAttributes(EnableAutoConfiguration.class.getName(),
							true));

			Assert.notNull(attributes, "No auto-configuration attributes found. Is "
					+ metadata.getClassName()
					+ " annotated with @EnableAutoConfiguration?");

			// Find all possible auto configuration classes, filtering duplicates
			List<String> factories = new ArrayList<String>(new LinkedHashSet<String>(
					SpringFactoriesLoader.loadFactoryNames(EnableAutoConfiguration.class,
							this.beanClassLoader)));

			// Remove those specifically disabled
			List<String> excluded = new ArrayList<String>(Arrays.asList(attributes
					.getStringArray("exclude")));
			factories.removeAll(excluded);
			ConditionEvaluationReport.get(this.beanFactory).recordExclusions(excluded);

			// Sort
			factories = new AutoConfigurationSorter(this.resourceLoader)
					.getInPriorityOrder(factories);

			return factories.toArray(new String[factories.size()]);
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.isInstanceOf(ConfigurableListableBeanFactory.class, beanFactory);
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
	}

}

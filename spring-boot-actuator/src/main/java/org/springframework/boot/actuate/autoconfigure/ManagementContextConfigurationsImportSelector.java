/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;

/**
 * Selects configuration classes for the management context configuration. Entries are
 * loaded from {@code /META-INF/spring.factories} under the
 * {@code org.springframework.boot.actuate.autoconfigure.ManagementContextConfiguration}
 * key.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @see ManagementContextConfiguration
 */
@Order(Ordered.LOWEST_PRECEDENCE)
class ManagementContextConfigurationsImportSelector
		implements DeferredImportSelector, BeanClassLoaderAware {

	private ClassLoader classLoader;

	@Override
	public String[] selectImports(AnnotationMetadata metadata) {
		// Find all management context configuration classes, filtering duplicates
		List<ManagementConfiguration> configurations = getConfigurations();
		OrderComparator.sort(configurations);
		List<String> names = new ArrayList<String>();
		for (ManagementConfiguration configuration : configurations) {
			names.add(configuration.getClassName());
		}
		return names.toArray(new String[names.size()]);
	}

	private List<ManagementConfiguration> getConfigurations() {
		SimpleMetadataReaderFactory readerFactory = new SimpleMetadataReaderFactory(
				this.classLoader);
		List<ManagementConfiguration> configurations = new ArrayList<ManagementConfiguration>();
		for (String className : loadFactoryNames()) {
			getConfiguration(readerFactory, configurations, className);
		}
		return configurations;
	}

	private void getConfiguration(SimpleMetadataReaderFactory readerFactory,
			List<ManagementConfiguration> configurations, String className) {
		try {
			MetadataReader metadataReader = readerFactory.getMetadataReader(className);
			configurations.add(new ManagementConfiguration(metadataReader));
		}
		catch (IOException ex) {
			throw new RuntimeException(
					"Failed to read annotation metadata for '" + className + "'", ex);
		}
	}

	protected List<String> loadFactoryNames() {
		return SpringFactoriesLoader
				.loadFactoryNames(ManagementContextConfiguration.class, this.classLoader);
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/**
	 * A management configuration class which can be sorted according to {@code @Order}.
	 */
	private static final class ManagementConfiguration implements Ordered {

		private final String className;

		private final int order;

		ManagementConfiguration(MetadataReader metadataReader) {
			AnnotationMetadata annotationMetadata = metadataReader
					.getAnnotationMetadata();
			this.order = readOrder(annotationMetadata);
			this.className = metadataReader.getClassMetadata().getClassName();
		}

		private int readOrder(AnnotationMetadata annotationMetadata) {
			Map<String, Object> attributes = annotationMetadata
					.getAnnotationAttributes(Order.class.getName());
			Integer order = (attributes != null) ? (Integer) attributes.get("value")
					: null;
			return (order != null) ? order : Ordered.LOWEST_PRECEDENCE;
		}

		public String getClassName() {
			return this.className;
		}

		@Override
		public int getOrder() {
			return this.order;
		}

	}

}

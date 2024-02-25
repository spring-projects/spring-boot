/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.web.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextType;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.util.StringUtils;

/**
 * Selects configuration classes for the management context configuration. Entries are
 * loaded from
 * {@code /META-INF/spring/org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration.imports}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 * @author Scott Frederick
 * @see ManagementContextConfiguration
 * @see ImportCandidates
 */
@Order(Ordered.LOWEST_PRECEDENCE)
class ManagementContextConfigurationImportSelector implements DeferredImportSelector, BeanClassLoaderAware {

	private ClassLoader classLoader;

	/**
	 * Selects the imports for the management context configuration classes based on the
	 * provided metadata.
	 * @param metadata the annotation metadata
	 * @return an array of fully qualified class names of the selected management context
	 * configuration classes
	 */
	@Override
	public String[] selectImports(AnnotationMetadata metadata) {
		ManagementContextType contextType = (ManagementContextType) metadata
			.getAnnotationAttributes(EnableManagementContext.class.getName())
			.get("value");
		// Find all management context configuration classes, filtering duplicates
		List<ManagementConfiguration> configurations = getConfigurations();
		OrderComparator.sort(configurations);
		List<String> names = new ArrayList<>();
		for (ManagementConfiguration configuration : configurations) {
			if (configuration.getContextType() == ManagementContextType.ANY
					|| configuration.getContextType() == contextType) {
				names.add(configuration.getClassName());
			}
		}
		return StringUtils.toStringArray(names);
	}

	/**
	 * Retrieves the list of ManagementConfigurations.
	 * @return the list of ManagementConfigurations
	 */
	private List<ManagementConfiguration> getConfigurations() {
		SimpleMetadataReaderFactory readerFactory = new SimpleMetadataReaderFactory(this.classLoader);
		List<ManagementConfiguration> configurations = new ArrayList<>();
		for (String className : loadFactoryNames()) {
			addConfiguration(readerFactory, configurations, className);
		}
		return configurations;
	}

	/**
	 * Adds a configuration to the list of management configurations.
	 * @param readerFactory the factory used to create metadata readers
	 * @param configurations the list of management configurations
	 * @param className the name of the class to read annotation metadata for
	 * @throws RuntimeException if failed to read annotation metadata for the specified
	 * class
	 */
	private void addConfiguration(SimpleMetadataReaderFactory readerFactory,
			List<ManagementConfiguration> configurations, String className) {
		try {
			MetadataReader metadataReader = readerFactory.getMetadataReader(className);
			configurations.add(new ManagementConfiguration(metadataReader));
		}
		catch (IOException ex) {
			throw new RuntimeException("Failed to read annotation metadata for '" + className + "'", ex);
		}
	}

	/**
	 * Loads the factory names from the {@link ManagementContextConfiguration} class using
	 * the provided class loader.
	 * @return a list of factory names
	 */
	protected List<String> loadFactoryNames() {
		return ImportCandidates.load(ManagementContextConfiguration.class, this.classLoader).getCandidates();
	}

	/**
	 * Sets the class loader to be used for loading beans.
	 * @param classLoader the class loader to be used for loading beans
	 */
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

		private final ManagementContextType contextType;

		/**
		 * Constructs a new ManagementConfiguration object with the provided metadata
		 * reader.
		 * @param metadataReader the metadata reader used to retrieve annotation and class
		 * metadata
		 */
		ManagementConfiguration(MetadataReader metadataReader) {
			AnnotationMetadata annotationMetadata = metadataReader.getAnnotationMetadata();
			this.order = readOrder(annotationMetadata);
			this.className = metadataReader.getClassMetadata().getClassName();
			this.contextType = readContextType(annotationMetadata);
		}

		/**
		 * Reads the context type from the given annotation metadata.
		 * @param annotationMetadata the annotation metadata to read from
		 * @return the management context type, defaulting to ManagementContextType.ANY if
		 * not specified
		 */
		private ManagementContextType readContextType(AnnotationMetadata annotationMetadata) {
			Map<String, Object> annotationAttributes = annotationMetadata
				.getAnnotationAttributes(ManagementContextConfiguration.class.getName());
			return (annotationAttributes != null) ? (ManagementContextType) annotationAttributes.get("value")
					: ManagementContextType.ANY;
		}

		/**
		 * Reads the order value from the given AnnotationMetadata object.
		 * @param annotationMetadata the AnnotationMetadata object to read the order value
		 * from
		 * @return the order value if present, otherwise returns the lowest precedence
		 * value
		 */
		private int readOrder(AnnotationMetadata annotationMetadata) {
			Map<String, Object> attributes = annotationMetadata.getAnnotationAttributes(Order.class.getName());
			Integer order = (attributes != null) ? (Integer) attributes.get("value") : null;
			return (order != null) ? order : Ordered.LOWEST_PRECEDENCE;
		}

		/**
		 * Returns the name of the class.
		 * @return the name of the class
		 */
		String getClassName() {
			return this.className;
		}

		/**
		 * Returns the order of the management configuration.
		 * @return the order of the management configuration
		 */
		@Override
		public int getOrder() {
			return this.order;
		}

		/**
		 * Returns the context type of the management configuration.
		 * @return the context type of the management configuration
		 */
		ManagementContextType getContextType() {
			return this.contextType;
		}

	}

}

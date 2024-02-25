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

package org.springframework.boot.autoconfigure;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

/**
 * A {@link TypeFilter} implementation that matches registered auto-configuration classes.
 *
 * @author Stephane Nicoll
 * @author Scott Frederick
 * @since 1.5.0
 */
public class AutoConfigurationExcludeFilter implements TypeFilter, BeanClassLoaderAware {

	private ClassLoader beanClassLoader;

	private volatile List<String> autoConfigurations;

	/**
     * Set the ClassLoader to be used for loading beans.
     * 
     * @param beanClassLoader the ClassLoader to be used for loading beans
     */
    @Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}

	/**
     * Determines if the given metadata reader matches the AutoConfigurationExcludeFilter criteria.
     * 
     * @param metadataReader the metadata reader to be checked
     * @param metadataReaderFactory the factory for creating metadata readers
     * @return true if the metadata reader matches the criteria, false otherwise
     * @throws IOException if an I/O error occurs while reading the metadata
     */
    @Override
	public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
			throws IOException {
		return isConfiguration(metadataReader) && isAutoConfiguration(metadataReader);
	}

	/**
     * Determines if the given metadata reader represents a configuration class.
     * 
     * @param metadataReader the metadata reader to check
     * @return {@code true} if the metadata reader is annotated with {@link Configuration}, {@code false} otherwise
     */
    private boolean isConfiguration(MetadataReader metadataReader) {
		return metadataReader.getAnnotationMetadata().isAnnotated(Configuration.class.getName());
	}

	/**
     * Checks if the given metadata reader is annotated with AutoConfiguration or if its class name is present in the list of auto configurations.
     * 
     * @param metadataReader the metadata reader to check
     * @return true if the metadata reader is annotated with AutoConfiguration or if its class name is present in the list of auto configurations, false otherwise
     */
    private boolean isAutoConfiguration(MetadataReader metadataReader) {
		boolean annotatedWithAutoConfiguration = metadataReader.getAnnotationMetadata()
			.isAnnotated(AutoConfiguration.class.getName());
		return annotatedWithAutoConfiguration
				|| getAutoConfigurations().contains(metadataReader.getClassMetadata().getClassName());
	}

	/**
     * Retrieves the list of auto configurations.
     * 
     * @return The list of auto configurations.
     */
    protected List<String> getAutoConfigurations() {
		if (this.autoConfigurations == null) {
			this.autoConfigurations = ImportCandidates.load(AutoConfiguration.class, this.beanClassLoader)
				.getCandidates();
		}
		return this.autoConfigurations;
	}

}

/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.context;

import java.io.IOException;
import java.util.Collection;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

/**
 * Provides exclusion {@link TypeFilter TypeFilters} that are loaded from the
 * {@link BeanFactory} and automatically applied to {@code SpringBootApplication}
 * scanning. Can also be used directly with {@code @ComponentScan} as follows:
 * <pre class="code">
 * &#064;ComponentScan(excludeFilters = @Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class))
 * </pre>
 * <p>
 * Implementations should provide a subclass registered with {@link BeanFactory} and
 * override the {@link #match(MetadataReader, MetadataReaderFactory)} method. They should
 * also implement a valid {@link #hashCode() hashCode} and {@link #equals(Object) equals}
 * methods so that they can be used as part of Spring test's application context caches.
 * <p>
 * Note that {@code TypeExcludeFilters} are initialized very early in the application
 * lifecycle, they should generally not have dependencies on any other beans. They are
 * primarily used internally to support {@code spring-boot-test}.
 *
 * @author Phillip Webb
 * @since 1.4.0
 */
public class TypeExcludeFilter implements TypeFilter, BeanFactoryAware {

	private BeanFactory beanFactory;

	private Collection<TypeExcludeFilter> delegates;

	/**
	 * Set the BeanFactory that this filter runs in.
	 * <p>
	 * Invoked after population of normal bean properties but before an init callback such
	 * as InitializingBean's {@code afterPropertiesSet} or a custom init-method. Invoked
	 * after the setting of any {@link ResourceLoaderAware},
	 * {@link ApplicationEventPublisherAware} or {@link MessageSourceAware} bean
	 * properties.
	 * <p>
	 * This method allows the filter to perform any initialization work necessary before
	 * filtering beans.
	 * @param beanFactory the BeanFactory that this filter runs in
	 * @throws BeansException if initialization failed
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	/**
	 * Determines if the given metadata reader matches the filter criteria.
	 * @param metadataReader the metadata reader to be matched
	 * @param metadataReaderFactory the factory for creating metadata readers
	 * @return true if the metadata reader matches the filter criteria, false otherwise
	 * @throws IOException if an I/O error occurs while reading the metadata
	 */
	@Override
	public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
			throws IOException {
		if (this.beanFactory instanceof ListableBeanFactory && getClass() == TypeExcludeFilter.class) {
			for (TypeExcludeFilter delegate : getDelegates()) {
				if (delegate.match(metadataReader, metadataReaderFactory)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns the collection of delegates for type exclusion filtering.
	 * @return the collection of delegates for type exclusion filtering
	 */
	private Collection<TypeExcludeFilter> getDelegates() {
		Collection<TypeExcludeFilter> delegates = this.delegates;
		if (delegates == null) {
			delegates = ((ListableBeanFactory) this.beanFactory).getBeansOfType(TypeExcludeFilter.class).values();
			this.delegates = delegates;
		}
		return delegates;
	}

	/**
	 * Compares this TypeExcludeFilter object with the specified object for equality.
	 * @param obj the object to compare with
	 * @return true if the specified object is equal to this TypeExcludeFilter object,
	 * false otherwise
	 * @throws IllegalStateException if the equals method is not implemented in the
	 * TypeExcludeFilter class
	 */
	@Override
	public boolean equals(Object obj) {
		throw new IllegalStateException("TypeExcludeFilter " + getClass() + " has not implemented equals");
	}

	/**
	 * Returns the hash code value for this TypeExcludeFilter object.
	 * @return the hash code value for this object
	 * @throws IllegalStateException if the TypeExcludeFilter class has not implemented
	 * hashCode
	 */
	@Override
	public int hashCode() {
		throw new IllegalStateException("TypeExcludeFilter " + getClass() + " has not implemented hashCode");
	}

}

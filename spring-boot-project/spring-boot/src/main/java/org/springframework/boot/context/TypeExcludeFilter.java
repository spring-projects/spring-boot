/*
 * Copyright 2012-2018 the original author or authors.
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

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public boolean match(MetadataReader metadataReader,
			MetadataReaderFactory metadataReaderFactory) throws IOException {
		if (this.beanFactory instanceof ListableBeanFactory
				&& getClass() == TypeExcludeFilter.class) {
			Collection<TypeExcludeFilter> delegates = ((ListableBeanFactory) this.beanFactory)
					.getBeansOfType(TypeExcludeFilter.class).values();
			for (TypeExcludeFilter delegate : delegates) {
				if (delegate.match(metadataReader, metadataReaderFactory)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean equals(Object obj) {
		throw new IllegalStateException(
				"TypeExcludeFilter " + getClass() + " has not implemented equals");
	}

	@Override
	public int hashCode() {
		throw new IllegalStateException(
				"TypeExcludeFilter " + getClass() + " has not implemented hashCode");
	}

}

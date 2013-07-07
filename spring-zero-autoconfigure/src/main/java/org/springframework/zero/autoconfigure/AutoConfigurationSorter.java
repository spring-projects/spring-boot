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

package org.springframework.zero.autoconfigure;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.util.Assert;

/**
 * Sort {@link EnableAutoConfiguration auto-configuration} classes into priority order by
 * reading {@link Ordered} and {@link AutoConfigureAfter} annotations (without loading
 * classes).
 * 
 * @author Phillip Webb
 */
class AutoConfigurationSorter {

	private CachingMetadataReaderFactory metadataReaderFactory;

	public AutoConfigurationSorter(ResourceLoader resourceLoader) {
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		this.metadataReaderFactory = new CachingMetadataReaderFactory(resourceLoader);
	}

	public List<String> getInPriorityOrder(Collection<String> classNames)
			throws IOException {
		List<AutoConfigurationClass> autoConfigurationClasses = new ArrayList<AutoConfigurationClass>();
		for (String className : classNames) {
			autoConfigurationClasses.add(new AutoConfigurationClass(className));
		}

		// Sort initially by order
		Collections.sort(autoConfigurationClasses, OrderComparator.INSTANCE);

		// Then respect @AutoConfigureAfter
		autoConfigurationClasses = sortByAfterAnnotation(autoConfigurationClasses);

		List<String> orderedClassNames = new ArrayList<String>();
		for (AutoConfigurationClass autoConfigurationClass : autoConfigurationClasses) {
			orderedClassNames.add(autoConfigurationClass.toString());
		}
		return orderedClassNames;
	}

	private List<AutoConfigurationClass> sortByAfterAnnotation(
			Collection<AutoConfigurationClass> autoConfigurationClasses)
			throws IOException {
		List<AutoConfigurationClass> tosort = new ArrayList<AutoConfigurationClass>(
				autoConfigurationClasses);
		Set<AutoConfigurationClass> sorted = new LinkedHashSet<AutoConfigurationClass>();
		Set<AutoConfigurationClass> processing = new LinkedHashSet<AutoConfigurationClass>();
		while (!tosort.isEmpty()) {
			doSortByAfterAnnotation(tosort, sorted, processing, null);
		}
		return new ArrayList<AutoConfigurationClass>(sorted);
	}

	private void doSortByAfterAnnotation(List<AutoConfigurationClass> tosort,
			Set<AutoConfigurationClass> sorted, Set<AutoConfigurationClass> processing,
			AutoConfigurationClass current) throws IOException {

		if (current == null) {
			current = tosort.remove(0);
		}

		processing.add(current);

		for (AutoConfigurationClass after : current.getAfter()) {
			Assert.state(!processing.contains(after),
					"Cycle @AutoConfigureAfter detected between " + current + " and "
							+ after);
			if (!sorted.contains(after) && tosort.contains(after)) {
				doSortByAfterAnnotation(tosort, sorted, processing, after);
			}
		}

		processing.remove(current);
		sorted.add(current);
	}

	private class AutoConfigurationClass implements Ordered {

		private final String className;

		private final int order;

		private List<AutoConfigurationClass> after;

		private Map<String, Object> afterAnnotation;

		public AutoConfigurationClass(String className) throws IOException {

			this.className = className;

			MetadataReader metadataReader = AutoConfigurationSorter.this.metadataReaderFactory
					.getMetadataReader(className);
			AnnotationMetadata metadata = metadataReader.getAnnotationMetadata();

			// Read @Order annotation
			Map<String, Object> orderedAnnotation = metadata
					.getAnnotationAttributes(Order.class.getName());
			this.order = (orderedAnnotation == null ? Ordered.LOWEST_PRECEDENCE
					: (Integer) orderedAnnotation.get("value"));

			// Read @AutoConfigureAfter annotation
			this.afterAnnotation = metadata.getAnnotationAttributes(
					AutoConfigureAfter.class.getName(), true);
		}

		@Override
		public int getOrder() {
			return this.order;
		}

		public List<AutoConfigurationClass> getAfter() throws IOException {
			if (this.after == null) {
				if (this.afterAnnotation == null) {
					this.after = Collections.emptyList();
				}
				else {
					this.after = new ArrayList<AutoConfigurationClass>();
					for (String afterClass : (String[]) this.afterAnnotation.get("value")) {
						this.after.add(new AutoConfigurationClass(afterClass));
					}
				}
			}
			return this.after;
		}

		@Override
		public String toString() {
			return this.className;
		}

		@Override
		public int hashCode() {
			return this.className.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return this.className.equals(((AutoConfigurationClass) obj).className);
		}
	}

}

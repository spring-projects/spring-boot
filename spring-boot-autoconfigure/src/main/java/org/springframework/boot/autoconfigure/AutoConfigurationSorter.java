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

package org.springframework.boot.autoconfigure;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.Ordered;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.Assert;

/**
 * Sort {@link EnableAutoConfiguration auto-configuration} classes into priority order by
 * reading {@link AutoConfigureOrder}, {@link AutoConfigureBefore} and
 * {@link AutoConfigureAfter} annotations (without loading classes).
 *
 * @author Phillip Webb
 */
class AutoConfigurationSorter {

	private final MetadataReaderFactory metadataReaderFactory;

	private final AutoConfigurationMetadata autoConfigurationMetadata;

	AutoConfigurationSorter(MetadataReaderFactory metadataReaderFactory,
			AutoConfigurationMetadata autoConfigurationMetadata) {
		Assert.notNull(metadataReaderFactory, "MetadataReaderFactory must not be null");
		this.metadataReaderFactory = metadataReaderFactory;
		this.autoConfigurationMetadata = autoConfigurationMetadata;
	}

	public List<String> getInPriorityOrder(Collection<String> classNames) {
		final AutoConfigurationClasses classes = new AutoConfigurationClasses(
				this.metadataReaderFactory, this.autoConfigurationMetadata, classNames);
		List<String> orderedClassNames = new ArrayList<String>(classNames);
		// Initially sort alphabetically
		Collections.sort(orderedClassNames);
		// Then sort by order
		Collections.sort(orderedClassNames, new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				int i1 = classes.get(o1).getOrder();
				int i2 = classes.get(o2).getOrder();
				return (i1 < i2) ? -1 : (i1 > i2) ? 1 : 0;
			}

		});
		// Then respect @AutoConfigureBefore @AutoConfigureAfter
		orderedClassNames = sortByAnnotation(classes, orderedClassNames);
		return orderedClassNames;
	}

	private List<String> sortByAnnotation(AutoConfigurationClasses classes,
			List<String> classNames) {
		List<String> toSort = new ArrayList<String>(classNames);
		Set<String> sorted = new LinkedHashSet<String>();
		Set<String> processing = new LinkedHashSet<String>();
		while (!toSort.isEmpty()) {
			doSortByAfterAnnotation(classes, toSort, sorted, processing, null);
		}
		return new ArrayList<String>(sorted);
	}

	private void doSortByAfterAnnotation(AutoConfigurationClasses classes,
			List<String> toSort, Set<String> sorted, Set<String> processing,
			String current) {
		if (current == null) {
			current = toSort.remove(0);
		}
		processing.add(current);
		for (String after : classes.getClassesRequestedAfter(current)) {
			Assert.state(!processing.contains(after),
					"AutoConfigure cycle detected between " + current + " and " + after);
			if (!sorted.contains(after) && toSort.contains(after)) {
				doSortByAfterAnnotation(classes, toSort, sorted, processing, after);
			}
		}
		processing.remove(current);
		sorted.add(current);
	}

	private static class AutoConfigurationClasses {

		private final Map<String, AutoConfigurationClass> classes = new HashMap<String, AutoConfigurationClass>();

		AutoConfigurationClasses(MetadataReaderFactory metadataReaderFactory,
				AutoConfigurationMetadata autoConfigurationMetadata,
				Collection<String> classNames) {
			for (String className : classNames) {
				this.classes.put(className, new AutoConfigurationClass(className,
						metadataReaderFactory, autoConfigurationMetadata));
			}
		}

		public AutoConfigurationClass get(String className) {
			return this.classes.get(className);
		}

		public Set<String> getClassesRequestedAfter(String className) {
			Set<String> rtn = new LinkedHashSet<String>();
			rtn.addAll(get(className).getAfter());
			for (Map.Entry<String, AutoConfigurationClass> entry : this.classes
					.entrySet()) {
				if (entry.getValue().getBefore().contains(className)) {
					rtn.add(entry.getKey());
				}
			}
			return rtn;
		}

	}

	private static class AutoConfigurationClass {

		private final String className;

		private final MetadataReaderFactory metadataReaderFactory;

		private final AutoConfigurationMetadata autoConfigurationMetadata;

		private AnnotationMetadata annotationMetadata;

		private final Set<String> before;

		private final Set<String> after;

		AutoConfigurationClass(String className,
				MetadataReaderFactory metadataReaderFactory,
				AutoConfigurationMetadata autoConfigurationMetadata) {
			this.className = className;
			this.metadataReaderFactory = metadataReaderFactory;
			this.autoConfigurationMetadata = autoConfigurationMetadata;
			this.before = readBefore();
			this.after = readAfter();
		}

		public Set<String> getBefore() {
			return this.before;
		}

		public Set<String> getAfter() {
			return this.after;
		}

		private int getOrder() {
			if (this.autoConfigurationMetadata.wasProcessed(this.className)) {
				return this.autoConfigurationMetadata.getInteger(this.className,
						"AutoConfigureOrder", Ordered.LOWEST_PRECEDENCE);
			}
			Map<String, Object> attributes = getAnnotationMetadata()
					.getAnnotationAttributes(AutoConfigureOrder.class.getName());
			return (attributes != null) ? (Integer) attributes.get("value")
					: Ordered.LOWEST_PRECEDENCE;
		}

		private Set<String> readBefore() {
			if (this.autoConfigurationMetadata.wasProcessed(this.className)) {
				return this.autoConfigurationMetadata.getSet(this.className,
						"AutoConfigureBefore", Collections.<String>emptySet());
			}
			return getAnnotationValue(AutoConfigureBefore.class);
		}

		private Set<String> readAfter() {
			if (this.autoConfigurationMetadata.wasProcessed(this.className)) {
				return this.autoConfigurationMetadata.getSet(this.className,
						"AutoConfigureAfter", Collections.<String>emptySet());
			}
			return getAnnotationValue(AutoConfigureAfter.class);
		}

		private Set<String> getAnnotationValue(Class<?> annotation) {
			Map<String, Object> attributes = getAnnotationMetadata()
					.getAnnotationAttributes(annotation.getName(), true);
			if (attributes == null) {
				return Collections.emptySet();
			}
			Set<String> value = new LinkedHashSet<String>();
			Collections.addAll(value, (String[]) attributes.get("value"));
			Collections.addAll(value, (String[]) attributes.get("name"));
			return value;
		}

		private AnnotationMetadata getAnnotationMetadata() {
			if (this.annotationMetadata == null) {
				try {
					MetadataReader metadataReader = this.metadataReaderFactory
							.getMetadataReader(this.className);
					this.annotationMetadata = metadataReader.getAnnotationMetadata();
				}
				catch (IOException ex) {
					throw new IllegalStateException(
							"Unable to read meta-data for class " + this.className, ex);
				}
			}
			return this.annotationMetadata;
		}

	}

}

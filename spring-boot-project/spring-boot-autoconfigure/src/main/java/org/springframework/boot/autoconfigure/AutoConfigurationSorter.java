/*
 * Copyright 2012-2025 the original author or authors.
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
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.Assert;

/**
 * Sort {@link EnableAutoConfiguration auto-configuration} classes into priority order by
 * reading {@link AutoConfigureOrder @AutoConfigureOrder},
 * {@link AutoConfigureBefore @AutoConfigureBefore} and
 * {@link AutoConfigureAfter @AutoConfigureAfter} annotations (without loading classes).
 *
 * @author Phillip Webb
 */
class AutoConfigurationSorter {

	private final MetadataReaderFactory metadataReaderFactory;

	private final AutoConfigurationMetadata autoConfigurationMetadata;

	private final UnaryOperator<String> replacementMapper;

	AutoConfigurationSorter(MetadataReaderFactory metadataReaderFactory,
			AutoConfigurationMetadata autoConfigurationMetadata, UnaryOperator<String> replacementMapper) {
		Assert.notNull(metadataReaderFactory, "'metadataReaderFactory' must not be null");
		this.metadataReaderFactory = metadataReaderFactory;
		this.autoConfigurationMetadata = autoConfigurationMetadata;
		this.replacementMapper = replacementMapper;
	}

	List<String> getInPriorityOrder(Collection<String> classNames) {
		// Initially sort alphabetically
		List<String> alphabeticallyOrderedClassNames = new ArrayList<>(classNames);
		Collections.sort(alphabeticallyOrderedClassNames);
		// Then sort by order
		AutoConfigurationClasses classes = new AutoConfigurationClasses(this.metadataReaderFactory,
				this.autoConfigurationMetadata, alphabeticallyOrderedClassNames);
		List<String> orderedClassNames = classNames.stream()
				// collect the changed className (e.g. for source style nested class name)
				.map(classes::getAutoConfigurationClassName)
				// sort by alphabetical order
				.sorted()
				// then by order
				.sorted((o1, o2) -> {
					int i1 = classes.get(o1).getOrder();
					int i2 = classes.get(o2).getOrder();
					return Integer.compare(i1, i2);
				}).collect(Collectors.toList());
		// Then respect @AutoConfigureBefore @AutoConfigureAfter
		orderedClassNames = sortByAnnotation(classes, orderedClassNames);
		return orderedClassNames;
	}

	private List<String> sortByAnnotation(AutoConfigurationClasses classes, List<String> classNames) {
		List<String> toSort = new ArrayList<>(classNames);
		toSort.addAll(classes.getAllNames());
		Set<String> sorted = new LinkedHashSet<>();
		Set<String> processing = new LinkedHashSet<>();
		while (!toSort.isEmpty()) {
			doSortByAfterAnnotation(classes, toSort, sorted, processing, null);
		}
		sorted.retainAll(classNames);
		return new ArrayList<>(sorted);
	}

	private void doSortByAfterAnnotation(AutoConfigurationClasses classes, List<String> toSort, Set<String> sorted,
			Set<String> processing, String current) {
		if (current == null) {
			current = toSort.remove(0);
		}
		processing.add(current);
		Set<String> afters = new TreeSet<>(Comparator.comparing(toSort::indexOf));
		afters.addAll(classes.getClassesRequestedAfter(current));
		for (String after : afters) {
			checkForCycles(processing, current, after);
			if (!sorted.contains(after) && toSort.contains(after)) {
				doSortByAfterAnnotation(classes, toSort, sorted, processing, after);
			}
		}
		processing.remove(current);
		sorted.add(current);
	}

	private void checkForCycles(Set<String> processing, String current, String after) {
		Assert.state(!processing.contains(after),
				() -> "AutoConfigure cycle detected between " + current + " and " + after);
	}

	private class AutoConfigurationClasses {

		private final Map<String, AutoConfigurationClass> classes = new LinkedHashMap<>();

		AutoConfigurationClasses(MetadataReaderFactory metadataReaderFactory,
				AutoConfigurationMetadata autoConfigurationMetadata, Collection<String> classNames) {
			addToClasses(metadataReaderFactory, autoConfigurationMetadata, classNames, true);
		}

		Set<String> getAllNames() {
			return this.classes.keySet();
		}

		private void addToClasses(MetadataReaderFactory metadataReaderFactory,
				AutoConfigurationMetadata autoConfigurationMetadata, Collection<String> classNames, boolean required) {
			for (String className : classNames) {
				if (!this.classes.containsKey(className)) {
					AutoConfigurationClass autoConfigurationClass = new AutoConfigurationClass(className,
							metadataReaderFactory, autoConfigurationMetadata);
					boolean available = autoConfigurationClass.isAvailable();
					if (required || available) {
						this.classes.put(className, autoConfigurationClass);
						// collect the changed className (e.g. for source style nested class name)
						if (!className.equals(autoConfigurationClass.getClassName())) {
							this.classes.put(autoConfigurationClass.getClassName(), autoConfigurationClass);
						}
					}
					if (available) {
						addToClasses(metadataReaderFactory, autoConfigurationMetadata,
								autoConfigurationClass.getBefore(), false);
						addToClasses(metadataReaderFactory, autoConfigurationMetadata,
								autoConfigurationClass.getAfter(), false);
					}
				}
			}
		}

		AutoConfigurationClass get(String className) {
			return this.classes.get(className);
		}

		String getAutoConfigurationClassName(String className) {
			AutoConfigurationClass autoConfigurationClass = this.classes.get(className);
			return null == autoConfigurationClass ? className : autoConfigurationClass.getClassName();
		}

		Set<String> getClassesRequestedAfter(String className) {
			Set<String> classesRequestedAfter = new LinkedHashSet<>(get(className).getAfter());
			this.classes.forEach((name, autoConfigurationClass) -> {
				if (autoConfigurationClass.getCorrectedBeforeClassNames(this::getAutoConfigurationClassName)
						.contains(className)) {
					classesRequestedAfter.add(name);
				}
			});
			return classesRequestedAfter;
		}

	}

	private class AutoConfigurationClass {

		private String className;

		private final MetadataReaderFactory metadataReaderFactory;

		private final AutoConfigurationMetadata autoConfigurationMetadata;

		private volatile AnnotationMetadata annotationMetadata;

		private volatile Set<String> before;

		private volatile Set<String> after;

		private volatile Set<String> correctedBeforeClassNames;

		AutoConfigurationClass(String className, MetadataReaderFactory metadataReaderFactory,
				AutoConfigurationMetadata autoConfigurationMetadata) {
			this.className = className;
			this.metadataReaderFactory = metadataReaderFactory;
			this.autoConfigurationMetadata = autoConfigurationMetadata;
		}

		public String getClassName() {
			// should be called after getAnnotationMetadata method for correcting className
			return this.className;
		}

		boolean isAvailable() {
			try {
				if (!wasProcessed()) {
					getAnnotationMetadata();
				}
				return true;
			}
			catch (Exception ex) {
				return false;
			}
		}

		Set<String> getBefore() {
			if (this.before == null) {
				this.before = getClassNames("AutoConfigureBefore", AutoConfigureBefore.class);
			}
			return this.before;
		}

		Set<String> getAfter() {
			if (this.after == null) {
				this.after = getClassNames("AutoConfigureAfter", AutoConfigureAfter.class);
			}
			return this.after;
		}

		private Set<String> getClassNames(String metadataKey, Class<? extends Annotation> annotation) {
			Set<String> annotationValue = wasProcessed()
					? this.autoConfigurationMetadata.getSet(this.className, metadataKey, Collections.emptySet())
					: getAnnotationValue(annotation);
			return applyReplacements(annotationValue);
		}

		private Set<String> applyReplacements(Set<String> values) {
			if (AutoConfigurationSorter.this.replacementMapper == null) {
				return values;
			}
			Set<String> replaced = new LinkedHashSet<>(values);
			for (String value : values) {
				replaced.add(AutoConfigurationSorter.this.replacementMapper.apply(value));
			}
			return replaced;
		}

		Set<String> getCorrectedBeforeClassNames(UnaryOperator<String> correctionMapper) {
			if (null == this.correctedBeforeClassNames) {
				this.correctedBeforeClassNames = getBefore().stream().map(correctionMapper)
						.collect(Collectors.toCollection(LinkedHashSet::new));
			}
			return this.correctedBeforeClassNames;
		}

		private int getOrder() {
			if (wasProcessed()) {
				return this.autoConfigurationMetadata.getInteger(this.className, "AutoConfigureOrder",
						AutoConfigureOrder.DEFAULT_ORDER);
			}
			Map<String, Object> attributes = getAnnotationMetadata()
				.getAnnotationAttributes(AutoConfigureOrder.class.getName());
			return (attributes != null) ? (Integer) attributes.get("value") : AutoConfigureOrder.DEFAULT_ORDER;
		}

		private boolean wasProcessed() {
			return (this.autoConfigurationMetadata != null
					&& this.autoConfigurationMetadata.wasProcessed(this.className));
		}

		private Set<String> getAnnotationValue(Class<?> annotation) {
			Map<String, Object> attributes = getAnnotationMetadata().getAnnotationAttributes(annotation.getName(),
					true);
			if (attributes == null) {
				return Collections.emptySet();
			}
			Set<String> value = new LinkedHashSet<>();
			Collections.addAll(value, (String[]) attributes.get("value"));
			Collections.addAll(value, (String[]) attributes.get("name"));
			return value;
		}

		private AnnotationMetadata getAnnotationMetadata() {
			if (this.annotationMetadata == null) {
				try {
					MetadataReader metadataReader = this.metadataReaderFactory.getMetadataReader(this.className);
					this.annotationMetadata = metadataReader.getAnnotationMetadata();
					// correct the class name (e.g. for source style nested class name)
					this.className = this.annotationMetadata.getClassName();
				}
				catch (IOException ex) {
					throw new IllegalStateException("Unable to read meta-data for class " + this.className, ex);
				}
			}
			return this.annotationMetadata;
		}

	}

}

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

	/**
     * Constructs a new AutoConfigurationSorter with the specified MetadataReaderFactory and AutoConfigurationMetadata.
     * 
     * @param metadataReaderFactory the MetadataReaderFactory to be used for reading metadata
     * @param autoConfigurationMetadata the AutoConfigurationMetadata to be used for auto-configuration metadata
     * @throws IllegalArgumentException if the metadataReaderFactory is null
     */
    AutoConfigurationSorter(MetadataReaderFactory metadataReaderFactory,
			AutoConfigurationMetadata autoConfigurationMetadata) {
		Assert.notNull(metadataReaderFactory, "MetadataReaderFactory must not be null");
		this.metadataReaderFactory = metadataReaderFactory;
		this.autoConfigurationMetadata = autoConfigurationMetadata;
	}

	/**
     * Returns a list of class names in priority order based on the given collection of class names.
     * The class names are initially sorted alphabetically and then sorted by their order.
     * The order is determined by the AutoConfigurationClasses object associated with each class name.
     * The class names are then sorted based on the @AutoConfigureBefore and @AutoConfigureAfter annotations.
     *
     * @param classNames the collection of class names to be sorted
     * @return a list of class names in priority order
     */
    List<String> getInPriorityOrder(Collection<String> classNames) {
		// Initially sort alphabetically
		List<String> alphabeticallyOrderedClassNames = new ArrayList<>(classNames);
		Collections.sort(alphabeticallyOrderedClassNames);
		// Then sort by order
		AutoConfigurationClasses classes = new AutoConfigurationClasses(this.metadataReaderFactory,
				this.autoConfigurationMetadata, alphabeticallyOrderedClassNames);
		List<String> orderedClassNames = new ArrayList<>(classNames);
		Collections.sort(orderedClassNames);
		orderedClassNames.sort((o1, o2) -> {
			int i1 = classes.get(o1).getOrder();
			int i2 = classes.get(o2).getOrder();
			return Integer.compare(i1, i2);
		});
		// Then respect @AutoConfigureBefore @AutoConfigureAfter
		orderedClassNames = sortByAnnotation(classes, orderedClassNames);
		return orderedClassNames;
	}

	/**
     * Sorts the given list of class names based on the presence of the @AfterAnnotation.
     * 
     * @param classes The AutoConfigurationClasses object containing the auto configuration classes.
     * @param classNames The list of class names to be sorted.
     * @return The sorted list of class names.
     */
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

	/**
     * Sorts the given list of strings based on the "after" annotation in the AutoConfigurationClasses.
     * 
     * @param classes the AutoConfigurationClasses object containing the requested classes and their "after" annotations
     * @param toSort the list of strings to be sorted
     * @param sorted the set of strings that have been sorted
     * @param processing the set of strings currently being processed
     * @param current the current string being processed
     */
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

	/**
     * Checks for cycles in the auto-configuration sorting process.
     * 
     * @param processing a set of auto-configuration classes that are currently being processed
     * @param current the current auto-configuration class being checked for cycles
     * @param after the auto-configuration class that comes after the current class in the sorting order
     * 
     * @throws IllegalStateException if a cycle is detected between the current class and the class that comes after it
     */
    private void checkForCycles(Set<String> processing, String current, String after) {
		Assert.state(!processing.contains(after),
				() -> "AutoConfigure cycle detected between " + current + " and " + after);
	}

	/**
     * AutoConfigurationClasses class.
     */
    private static class AutoConfigurationClasses {

		private final Map<String, AutoConfigurationClass> classes = new LinkedHashMap<>();

		/**
         * Adds the given collection of class names to the auto-configuration classes.
         * 
         * @param metadataReaderFactory the factory used to read metadata from class files
         * @param autoConfigurationMetadata the metadata for auto-configuration classes
         * @param classNames the collection of class names to be added
         */
        AutoConfigurationClasses(MetadataReaderFactory metadataReaderFactory,
				AutoConfigurationMetadata autoConfigurationMetadata, Collection<String> classNames) {
			addToClasses(metadataReaderFactory, autoConfigurationMetadata, classNames, true);
		}

		/**
         * Returns a set of all names in the classes map.
         *
         * @return a set of all names in the classes map
         */
        Set<String> getAllNames() {
			return this.classes.keySet();
		}

		/**
         * Adds the given class names to the classes map in the AutoConfigurationClasses class.
         * 
         * @param metadataReaderFactory The metadata reader factory used to read the class metadata.
         * @param autoConfigurationMetadata The auto configuration metadata.
         * @param classNames The collection of class names to be added.
         * @param required Indicates whether the class is required or not.
         */
        private void addToClasses(MetadataReaderFactory metadataReaderFactory,
				AutoConfigurationMetadata autoConfigurationMetadata, Collection<String> classNames, boolean required) {
			for (String className : classNames) {
				if (!this.classes.containsKey(className)) {
					AutoConfigurationClass autoConfigurationClass = new AutoConfigurationClass(className,
							metadataReaderFactory, autoConfigurationMetadata);
					boolean available = autoConfigurationClass.isAvailable();
					if (required || available) {
						this.classes.put(className, autoConfigurationClass);
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

		/**
         * Retrieves the AutoConfigurationClass object associated with the specified class name.
         * 
         * @param className the name of the class to retrieve the AutoConfigurationClass object for
         * @return the AutoConfigurationClass object associated with the specified class name, or null if not found
         */
        AutoConfigurationClass get(String className) {
			return this.classes.get(className);
		}

		/**
         * Returns a set of classes that are requested after the given class name.
         * 
         * @param className the name of the class
         * @return a set of classes requested after the given class name
         */
        Set<String> getClassesRequestedAfter(String className) {
			Set<String> classesRequestedAfter = new LinkedHashSet<>(get(className).getAfter());
			this.classes.forEach((name, autoConfigurationClass) -> {
				if (autoConfigurationClass.getBefore().contains(className)) {
					classesRequestedAfter.add(name);
				}
			});
			return classesRequestedAfter;
		}

	}

	/**
     * AutoConfigurationClass class.
     */
    private static class AutoConfigurationClass {

		private final String className;

		private final MetadataReaderFactory metadataReaderFactory;

		private final AutoConfigurationMetadata autoConfigurationMetadata;

		private volatile AnnotationMetadata annotationMetadata;

		private volatile Set<String> before;

		private volatile Set<String> after;

		/**
         * Constructs a new AutoConfigurationClass with the specified class name, metadata reader factory, and auto configuration metadata.
         * 
         * @param className the fully qualified name of the class
         * @param metadataReaderFactory the factory used to create metadata readers
         * @param autoConfigurationMetadata the metadata for auto configuration
         */
        AutoConfigurationClass(String className, MetadataReaderFactory metadataReaderFactory,
				AutoConfigurationMetadata autoConfigurationMetadata) {
			this.className = className;
			this.metadataReaderFactory = metadataReaderFactory;
			this.autoConfigurationMetadata = autoConfigurationMetadata;
		}

		/**
         * Checks if the AutoConfigurationClass is available.
         * 
         * @return true if the AutoConfigurationClass is available, false otherwise
         */
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

		/**
         * Returns the set of classes that this auto-configuration class should be configured before.
         * If the 'before' set is not already initialized, it will be initialized based on the following conditions:
         * - If the auto-configuration class has already been processed, the 'before' set will be retrieved from the auto-configuration metadata.
         * - If the auto-configuration class has not been processed, the 'before' set will be retrieved from the @AutoConfigureBefore annotation.
         *
         * @return the set of classes that this auto-configuration class should be configured before
         */
        Set<String> getBefore() {
			if (this.before == null) {
				this.before = (wasProcessed() ? this.autoConfigurationMetadata.getSet(this.className,
						"AutoConfigureBefore", Collections.emptySet()) : getAnnotationValue(AutoConfigureBefore.class));
			}
			return this.before;
		}

		/**
         * Returns the set of class names that this auto-configuration class should be configured after.
         * If the 'after' set is not already initialized, it will be initialized based on the following conditions:
         * - If the auto-configuration class has already been processed, the 'after' set will be retrieved from the auto-configuration metadata.
         * - If the auto-configuration class has not been processed yet, the 'after' set will be retrieved from the @AutoConfigureAfter annotation.
         * 
         * @return the set of class names that this auto-configuration class should be configured after
         */
        Set<String> getAfter() {
			if (this.after == null) {
				this.after = (wasProcessed() ? this.autoConfigurationMetadata.getSet(this.className,
						"AutoConfigureAfter", Collections.emptySet()) : getAnnotationValue(AutoConfigureAfter.class));
			}
			return this.after;
		}

		/**
         * Returns the order value for the auto-configuration class.
         * If the auto-configuration class has been processed, the order value is retrieved from the autoConfigurationMetadata.
         * Otherwise, the order value is retrieved from the annotation attributes of the AutoConfigureOrder annotation.
         * If no order value is found, the default order value is returned.
         *
         * @return the order value for the auto-configuration class
         */
        private int getOrder() {
			if (wasProcessed()) {
				return this.autoConfigurationMetadata.getInteger(this.className, "AutoConfigureOrder",
						AutoConfigureOrder.DEFAULT_ORDER);
			}
			Map<String, Object> attributes = getAnnotationMetadata()
				.getAnnotationAttributes(AutoConfigureOrder.class.getName());
			return (attributes != null) ? (Integer) attributes.get("value") : AutoConfigureOrder.DEFAULT_ORDER;
		}

		/**
         * Returns a boolean value indicating whether the current class was processed.
         * 
         * @return {@code true} if the class was processed, {@code false} otherwise.
         */
        private boolean wasProcessed() {
			return (this.autoConfigurationMetadata != null
					&& this.autoConfigurationMetadata.wasProcessed(this.className));
		}

		/**
         * Retrieves the value of the specified annotation.
         * 
         * @param annotation the annotation class to retrieve the value from
         * @return a set of strings representing the values of the annotation
         */
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

		/**
         * Retrieves the annotation metadata for the current class.
         * 
         * @return the annotation metadata
         * @throws IllegalStateException if unable to read meta-data for the class
         */
        private AnnotationMetadata getAnnotationMetadata() {
			if (this.annotationMetadata == null) {
				try {
					MetadataReader metadataReader = this.metadataReaderFactory.getMetadataReader(this.className);
					this.annotationMetadata = metadataReader.getAnnotationMetadata();
				}
				catch (IOException ex) {
					throw new IllegalStateException("Unable to read meta-data for class " + this.className, ex);
				}
			}
			return this.annotationMetadata;
		}

	}

}

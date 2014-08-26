/*
 * Copyright 2012-2014 the original author or authors.
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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.Assert;

/**
 * Sort {@link EnableAutoConfiguration auto-configuration} classes into priority order by
 * reading {@link Ordered}, {@link AutoConfigureBefore} and {@link AutoConfigureAfter}
 * annotations (without loading classes).
 *
 * @author Phillip Webb
 */
class AutoConfigurationSorter {

	private final CachingMetadataReaderFactory metadataReaderFactory;

	public AutoConfigurationSorter(ResourceLoader resourceLoader) {
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		this.metadataReaderFactory = new CachingMetadataReaderFactory(resourceLoader);
	}

	public List<String> getInPriorityOrder(Collection<String> classNames)
			throws IOException {

		final AutoConfigurationClasses classes = new AutoConfigurationClasses(
				this.metadataReaderFactory, classNames);

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
		List<String> tosort = new ArrayList<String>(classNames);
		Set<String> sorted = new LinkedHashSet<String>();
		Set<String> processing = new LinkedHashSet<String>();
		while (!tosort.isEmpty()) {
			doSortByAfterAnnotation(classes, tosort, sorted, processing, null);
		}
		return new ArrayList<String>(sorted);
	}

	private void doSortByAfterAnnotation(AutoConfigurationClasses classes,
			List<String> tosort, Set<String> sorted, Set<String> processing,
			String current) {

		if (current == null) {
			current = tosort.remove(0);
		}

		processing.add(current);

		for (String packageName : classes.getPackagesRequestedAfter(current)) {
			Assert.state(!processing.contains(packageName), "AutoConfigure cycle detected between " + current + " and "
					+ packageName);
			if (!sorted.contains(packageName) && tosort.contains(packageName)) {
				doSortByAfterAnnotation(classes, tosort, sorted, processing, packageName);
			}
		}

		for (String after : classes.getClassesRequestedAfter(current)) {
			Assert.state(!processing.contains(after),
					"AutoConfigure cycle detected between " + current + " and " + after);
			if (!sorted.contains(after) && tosort.contains(after)) {
				doSortByAfterAnnotation(classes, tosort, sorted, processing, after);
			}
		}

		processing.remove(current);
		sorted.add(current);
	}

	@SuppressWarnings("rawtypes")
	public static List<String> getClasses(String packageName)
			throws IOException, ClassNotFoundException {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		String path = packageName.replace(".", "/");
		Enumeration<URL> resources = classLoader.getResources(path);
		List<File> dirs = new ArrayList<File>();
		while (resources.hasMoreElements()) {
			URL urlResource = resources.nextElement();
			dirs.add(new File(urlResource.getFile()));
		}
		List<String> classes = new ArrayList<String>();
		for (File directory : dirs) {
			classes.addAll(findClasses(directory, packageName));
		}
		return classes;
	}

	@SuppressWarnings("rawtypes")
	private static List<String> findClasses(File directory, String packageName)
			throws ClassNotFoundException {
		List<String> classes = new ArrayList<String>();
		if (!directory.isDirectory()) {
			return classes;
		}
		File[] files = directory.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				assert !file.getName().contains(".");
				classes.addAll(findClasses(file, packageName + "." + file.getName()));
			} 
			else if (file.getName().endsWith("AutoConfiguration.class")) {
				classes.add(packageName
						+ "."
						+ file.getName().substring(0, file.getName().length() - 6));
			}
		}
		return classes;
	}

	private static class AutoConfigurationClasses {

		private final Map<String, AutoConfigurationClass> classes = new HashMap<String, AutoConfigurationClass>();

		public AutoConfigurationClasses(MetadataReaderFactory metadataReaderFactory,
				Collection<String> classNames) throws IOException {
			for (String className : classNames) {
				MetadataReader metadataReader = metadataReaderFactory
						.getMetadataReader(className);
				this.classes.put(className, new AutoConfigurationClass(metadataReader));
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

		public Set<String> getPackagesRequestedAfter(String className) {
			Set<String> rtn = new LinkedHashSet<String>();
			try {
				for (String packageName : get(className).getPackageAfter()) {
					rtn.addAll(getClasses(packageName));
				}
			} 
			catch (ClassNotFoundException e) {
				return null;
			} 
			catch (IOException e) {
				return null;
			}
			for (Map.Entry<String, AutoConfigurationClass> entry : this.classes.entrySet()) {
				if (className.indexOf(".") > 0) {
					String packageName = className.substring(0, className.lastIndexOf("."));
					if (entry.getValue().getPackageBefore().contains(packageName)) {
						rtn.add(entry.getKey());
					}
				}
			}
			return rtn;
		}
	}

	private static class AutoConfigurationClass {

		private final AnnotationMetadata metadata;

		public AutoConfigurationClass(MetadataReader metadataReader) {
			this.metadata = metadataReader.getAnnotationMetadata();
		}

		public int getOrder() {
			Map<String, Object> orderedAnnotation = this.metadata
					.getAnnotationAttributes(Order.class.getName());
			return (orderedAnnotation == null ? Ordered.LOWEST_PRECEDENCE
					: (Integer) orderedAnnotation.get("value"));
		}

		public Set<String> getBefore() {
			return getAnnotationValue(AutoConfigureBefore.class);
		}

		public Set<String> getAfter() {
			return getAnnotationValue(AutoConfigureAfter.class);
		}

		public Set<String> getPackageBefore() {
			return getAnnotationPackage(AutoConfigureBefore.class);
		}

		public Set<String> getPackageAfter() {
			return getAnnotationPackage(AutoConfigureAfter.class);
		}

		private Set<String> getAnnotationValue(Class<?> annotation) {
			Map<String, Object> attributes = this.metadata.getAnnotationAttributes(
					annotation.getName(), true);
			if (attributes == null) {
				return Collections.emptySet();
			}
			return new HashSet<String>(Arrays.asList((String[]) attributes.get("value")));
		}

		private Set<String> getAnnotationPackage(Class<?> annotation) {
			Map<String, Object> attributes = this.metadata
					.getAnnotationAttributes(annotation.getName(), true);
			if (attributes == null) {
				return Collections.emptySet();
			}
			return new HashSet<String>(Arrays.asList((String[]) attributes
					.get("packages")));
		}

	}

}

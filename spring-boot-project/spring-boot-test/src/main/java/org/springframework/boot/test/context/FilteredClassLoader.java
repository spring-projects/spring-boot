/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.test.context;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.function.Predicate;

import org.springframework.core.SmartClassLoader;
import org.springframework.core.io.ClassPathResource;

/**
 * Test {@link URLClassLoader} that can filter the classes and resources it can load.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Roy Jacobs
 * @since 2.0.0
 */
public class FilteredClassLoader extends URLClassLoader implements SmartClassLoader {

	private final Collection<Predicate<String>> classesFilters;

	private final Collection<Predicate<String>> resourcesFilters;

	/**
	 * Create a {@link FilteredClassLoader} that hides the given classes.
	 * @param hiddenClasses the classes to hide
	 */
	public FilteredClassLoader(Class<?>... hiddenClasses) {
		this(Collections.singleton(ClassFilter.of(hiddenClasses)), Collections.emptyList());
	}

	/**
	 * Create a {@link FilteredClassLoader} that hides classes from the given packages.
	 * @param hiddenPackages the packages to hide
	 */
	public FilteredClassLoader(String... hiddenPackages) {
		this(Collections.singleton(PackageFilter.of(hiddenPackages)), Collections.emptyList());
	}

	/**
	 * Create a {@link FilteredClassLoader} that hides resources from the given
	 * {@link ClassPathResource classpath resources}.
	 * @param hiddenResources the resources to hide
	 * @since 2.1.0
	 */
	public FilteredClassLoader(ClassPathResource... hiddenResources) {
		this(Collections.emptyList(), Collections.singleton(ClassPathResourceFilter.of(hiddenResources)));
	}

	/**
	 * Create a {@link FilteredClassLoader} that filters based on the given predicate.
	 * @param filters a set of filters to determine when a class name or resource should
	 * be hidden. A {@link Predicate#test(Object) result} of {@code true} indicates a
	 * filtered class or resource. The input of the predicate can either be the binary
	 * name of a class or a resource name.
	 */
	@SafeVarargs
	@SuppressWarnings("varargs")
	public FilteredClassLoader(Predicate<String>... filters) {
		this(Arrays.asList(filters), Arrays.asList(filters));
	}

	/**
	 * Constructs a new FilteredClassLoader with the specified collection of class filters
	 * and resource filters.
	 * @param classesFilters the collection of predicates used to filter classes
	 * @param resourcesFilters the collection of predicates used to filter resources
	 */
	private FilteredClassLoader(Collection<Predicate<String>> classesFilters,
			Collection<Predicate<String>> resourcesFilters) {
		super(new URL[0], FilteredClassLoader.class.getClassLoader());
		this.classesFilters = classesFilters;
		this.resourcesFilters = resourcesFilters;
	}

	/**
	 * Loads the class with the specified name, applying the defined filters.
	 * @param name the name of the class to be loaded
	 * @param resolve indicates whether or not to resolve the class
	 * @return the loaded class
	 * @throws ClassNotFoundException if the class is filtered and cannot be loaded
	 */
	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		for (Predicate<String> filter : this.classesFilters) {
			if (filter.test(name)) {
				throw new ClassNotFoundException();
			}
		}
		return super.loadClass(name, resolve);
	}

	/**
	 * Returns a URL object representing the resource with the given name.
	 *
	 * This method checks if the resource name passes any of the registered filters. If a
	 * filter matches the name, null is returned indicating that the resource is not
	 * accessible. If none of the filters match, the method delegates to the superclass
	 * implementation to retrieve the resource.
	 * @param name the name of the resource
	 * @return a URL object representing the resource, or null if the resource is not
	 * accessible
	 */
	@Override
	public URL getResource(String name) {
		for (Predicate<String> filter : this.resourcesFilters) {
			if (filter.test(name)) {
				return null;
			}
		}
		return super.getResource(name);
	}

	/**
	 * Returns an enumeration of URLs representing all the resources with the given name.
	 * @param name the name of the resource
	 * @return an enumeration of URLs representing all the resources with the given name
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		for (Predicate<String> filter : this.resourcesFilters) {
			if (filter.test(name)) {
				return Collections.emptyEnumeration();
			}
		}
		return super.getResources(name);
	}

	/**
	 * Retrieves an input stream for reading the specified resource.
	 * @param name the name of the resource
	 * @return an input stream for reading the resource, or null if the resource is
	 * filtered
	 */
	@Override
	public InputStream getResourceAsStream(String name) {
		for (Predicate<String> filter : this.resourcesFilters) {
			if (filter.test(name)) {
				return null;
			}
		}
		return super.getResourceAsStream(name);
	}

	/**
	 * Defines a public class with the given name, byte array, and protection domain.
	 * @param name the name of the class to be defined
	 * @param b the byte array containing the class data
	 * @param protectionDomain the protection domain for the defined class
	 * @return the defined class
	 * @throws IllegalArgumentException if defining a class with the given name is not
	 * supported
	 */
	@Override
	public Class<?> publicDefineClass(String name, byte[] b, ProtectionDomain protectionDomain) {
		for (Predicate<String> filter : this.classesFilters) {
			if (filter.test(name)) {
				throw new IllegalArgumentException(String.format("Defining class with name %s is not supported", name));
			}
		}
		return defineClass(name, b, 0, b.length, protectionDomain);
	}

	/**
	 * Filter to restrict the classes that can be loaded.
	 */
	public static final class ClassFilter implements Predicate<String> {

		private final Class<?>[] hiddenClasses;

		/**
		 * Constructs a new ClassFilter object with the specified hidden classes.
		 * @param hiddenClasses an array of Class objects representing the classes to be
		 * hidden
		 */
		private ClassFilter(Class<?>[] hiddenClasses) {
			this.hiddenClasses = hiddenClasses;
		}

		/**
		 * Tests if a given class name matches any of the hidden classes.
		 * @param className the name of the class to be tested
		 * @return true if the class name matches any of the hidden classes, false
		 * otherwise
		 */
		@Override
		public boolean test(String className) {
			for (Class<?> hiddenClass : this.hiddenClasses) {
				if (className.equals(hiddenClass.getName())) {
					return true;
				}
			}
			return false;
		}

		/**
		 * Creates a new instance of ClassFilter with the specified hidden classes.
		 * @param hiddenClasses the classes to be hidden by the ClassFilter
		 * @return a new instance of ClassFilter
		 */
		public static ClassFilter of(Class<?>... hiddenClasses) {
			return new ClassFilter(hiddenClasses);
		}

	}

	/**
	 * Filter to restrict the packages that can be loaded.
	 */
	public static final class PackageFilter implements Predicate<String> {

		private final String[] hiddenPackages;

		/**
		 * Constructs a new PackageFilter object with the specified hidden packages.
		 * @param hiddenPackages an array of strings representing the packages to be
		 * hidden
		 */
		private PackageFilter(String[] hiddenPackages) {
			this.hiddenPackages = hiddenPackages;
		}

		/**
		 * Tests if a given class name belongs to any of the hidden packages.
		 * @param className the name of the class to be tested
		 * @return true if the class belongs to a hidden package, false otherwise
		 */
		@Override
		public boolean test(String className) {
			for (String hiddenPackage : this.hiddenPackages) {
				if (className.startsWith(hiddenPackage)) {
					return true;
				}
			}
			return false;
		}

		/**
		 * Creates a new PackageFilter object with the specified hidden packages.
		 * @param hiddenPackages the packages to be hidden
		 * @return a new PackageFilter object
		 */
		public static PackageFilter of(String... hiddenPackages) {
			return new PackageFilter(hiddenPackages);
		}

	}

	/**
	 * Filter to restrict the resources that can be loaded.
	 *
	 * @since 2.1.0
	 */
	public static final class ClassPathResourceFilter implements Predicate<String> {

		private final ClassPathResource[] hiddenResources;

		/**
		 * Constructs a new ClassPathResourceFilter with the specified hidden resources.
		 * @param hiddenResources an array of ClassPathResource objects representing the
		 * resources to be hidden
		 */
		private ClassPathResourceFilter(ClassPathResource[] hiddenResources) {
			this.hiddenResources = hiddenResources;
		}

		/**
		 * Tests if a given resource name is hidden.
		 * @param resourceName the name of the resource to be tested
		 * @return true if the resource is hidden, false otherwise
		 */
		@Override
		public boolean test(String resourceName) {
			for (ClassPathResource hiddenResource : this.hiddenResources) {
				if (hiddenResource.getFilename() != null && resourceName.equals(hiddenResource.getPath())) {
					return true;
				}
			}
			return false;
		}

		/**
		 * Creates a new instance of ClassPathResourceFilter with the specified hidden
		 * resources.
		 * @param hiddenResources the hidden resources to be filtered
		 * @return a new instance of ClassPathResourceFilter
		 */
		public static ClassPathResourceFilter of(ClassPathResource... hiddenResources) {
			return new ClassPathResourceFilter(hiddenResources);
		}

	}

}

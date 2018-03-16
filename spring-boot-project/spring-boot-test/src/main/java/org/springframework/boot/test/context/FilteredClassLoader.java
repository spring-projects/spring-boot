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

package org.springframework.boot.test.context;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.function.Predicate;

/**
 * Test {@link URLClassLoader} that can filter the classes it can load.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 2.0.0
 */
public class FilteredClassLoader extends URLClassLoader {

	private final Predicate<String>[] filters;

	/**
	 * Create a {@link FilteredClassLoader} that hides the given classes.
	 * @param hiddenClasses the classes to hide
	 */
	public FilteredClassLoader(Class<?>... hiddenClasses) {
		this(ClassFilter.of(hiddenClasses));
	}

	/**
	 * Create a {@link FilteredClassLoader} that hides classes from the given packages.
	 * @param hiddenPackages the packages to hide
	 */
	public FilteredClassLoader(String... hiddenPackages) {
		this(PackageFilter.of(hiddenPackages));
	}

	/**
	 * Create a {@link FilteredClassLoader} that filters based on the given predicate.
	 * @param filters a set of filters to determine when a class name should be hidden. A
	 * {@link Predicate#test(Object) result} of {@code true} indicates a filtered class.
	 */
	@SafeVarargs
	public FilteredClassLoader(Predicate<String>... filters) {
		super(new URL[0], FilteredClassLoader.class.getClassLoader());
		this.filters = filters;
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve)
			throws ClassNotFoundException {
		for (Predicate<String> filter : this.filters) {
			if (filter.test(name)) {
				throw new ClassNotFoundException();
			}
		}
		return super.loadClass(name, resolve);
	}

	/**
	 * Filter to restrict the classes that can be loaded.
	 */
	public static final class ClassFilter implements Predicate<String> {

		private Class<?>[] hiddenClasses;

		private ClassFilter(Class<?>[] hiddenClasses) {
			this.hiddenClasses = hiddenClasses;
		}

		@Override
		public boolean test(String className) {
			for (Class<?> hiddenClass : this.hiddenClasses) {
				if (className.equals(hiddenClass.getName())) {
					return true;
				}
			}
			return false;
		}

		public static ClassFilter of(Class<?>... hiddenClasses) {
			return new ClassFilter(hiddenClasses);
		}

	}

	/**
	 * Filter to restrict the packages that can be loaded.
	 */
	public static final class PackageFilter implements Predicate<String> {

		private final String[] hiddenPackages;

		private PackageFilter(String[] hiddenPackages) {
			this.hiddenPackages = hiddenPackages;
		}

		@Override
		public boolean test(String className) {
			for (String hiddenPackage : this.hiddenPackages) {
				if (className.startsWith(hiddenPackage)) {
					return true;
				}
			}
			return false;
		}

		public static PackageFilter of(String... hiddenPackages) {
			return new PackageFilter(hiddenPackages);
		}

	}
}

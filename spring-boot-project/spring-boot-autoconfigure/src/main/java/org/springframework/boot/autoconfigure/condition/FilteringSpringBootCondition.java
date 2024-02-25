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

package org.springframework.boot.autoconfigure.condition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

/**
 * Abstract base class for a {@link SpringBootCondition} that also implements
 * {@link AutoConfigurationImportFilter}.
 *
 * @author Phillip Webb
 */
abstract class FilteringSpringBootCondition extends SpringBootCondition
		implements AutoConfigurationImportFilter, BeanFactoryAware, BeanClassLoaderAware {

	private BeanFactory beanFactory;

	private ClassLoader beanClassLoader;

	/**
     * Matches the given auto configuration classes against the auto configuration metadata.
     * 
     * @param autoConfigurationClasses the array of auto configuration classes to match
     * @param autoConfigurationMetadata the auto configuration metadata to match against
     * @return an array of booleans indicating whether each auto configuration class matches or not
     */
    @Override
	public boolean[] match(String[] autoConfigurationClasses, AutoConfigurationMetadata autoConfigurationMetadata) {
		ConditionEvaluationReport report = ConditionEvaluationReport.find(this.beanFactory);
		ConditionOutcome[] outcomes = getOutcomes(autoConfigurationClasses, autoConfigurationMetadata);
		boolean[] match = new boolean[outcomes.length];
		for (int i = 0; i < outcomes.length; i++) {
			match[i] = (outcomes[i] == null || outcomes[i].isMatch());
			if (!match[i] && outcomes[i] != null) {
				logOutcome(autoConfigurationClasses[i], outcomes[i]);
				if (report != null) {
					report.recordConditionEvaluation(autoConfigurationClasses[i], this, outcomes[i]);
				}
			}
		}
		return match;
	}

	/**
     * Returns an array of ConditionOutcome objects based on the provided auto configuration classes and metadata.
     *
     * @param autoConfigurationClasses   an array of auto configuration classes
     * @param autoConfigurationMetadata the auto configuration metadata
     * @return an array of ConditionOutcome objects
     */
    protected abstract ConditionOutcome[] getOutcomes(String[] autoConfigurationClasses,
			AutoConfigurationMetadata autoConfigurationMetadata);

	/**
     * Sets the bean factory for this FilteringSpringBootCondition.
     * 
     * @param beanFactory the bean factory to set
     * @throws BeansException if an error occurs while setting the bean factory
     */
    @Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	/**
     * Returns the BeanFactory associated with this FilteringSpringBootCondition.
     *
     * @return the BeanFactory associated with this FilteringSpringBootCondition
     */
    protected final BeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	/**
     * Returns the class loader used for loading the beans.
     *
     * @return the class loader used for loading the beans
     */
    protected final ClassLoader getBeanClassLoader() {
		return this.beanClassLoader;
	}

	/**
     * Sets the class loader to be used for loading beans.
     * 
     * @param classLoader the class loader to be set
     */
    @Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	/**
     * Filters a collection of class names based on a given class name filter and class loader.
     * 
     * @param classNames the collection of class names to be filtered
     * @param classNameFilter the class name filter to be applied
     * @param classLoader the class loader to be used for filtering
     * @return a list of class names that match the given filter and class loader
     */
    protected final List<String> filter(Collection<String> classNames, ClassNameFilter classNameFilter,
			ClassLoader classLoader) {
		if (CollectionUtils.isEmpty(classNames)) {
			return Collections.emptyList();
		}
		List<String> matches = new ArrayList<>(classNames.size());
		for (String candidate : classNames) {
			if (classNameFilter.matches(candidate, classLoader)) {
				matches.add(candidate);
			}
		}
		return matches;
	}

	/**
	 * Slightly faster variant of {@link ClassUtils#forName(String, ClassLoader)} that
	 * doesn't deal with primitives, arrays or inner types.
	 * @param className the class name to resolve
	 * @param classLoader the class loader to use
	 * @return a resolved class
	 * @throws ClassNotFoundException if the class cannot be found
	 */
	protected static Class<?> resolve(String className, ClassLoader classLoader) throws ClassNotFoundException {
		if (classLoader != null) {
			return Class.forName(className, false, classLoader);
		}
		return Class.forName(className);
	}

	protected enum ClassNameFilter {

		PRESENT {

			/**
     * Determines if the given class name is present in the specified class loader.
     * 
     * @param className the name of the class to check
     * @param classLoader the class loader to search in
     * @return true if the class is present, false otherwise
     */
    @Override
			public boolean matches(String className, ClassLoader classLoader) {
				return isPresent(className, classLoader);
			}

		},

		MISSING {

			/**
     * Determines if the given class name matches the condition by checking if it is not present in the specified class loader.
     * 
     * @param className the name of the class to check
     * @param classLoader the class loader to use for checking
     * @return true if the class name does not exist in the class loader, false otherwise
     */
    @Override
			public boolean matches(String className, ClassLoader classLoader) {
				return !isPresent(className, classLoader);
			}

		};

		/**
     * Checks if the given class name matches the specified class loader.
     *
     * @param className   the name of the class to be checked
     * @param classLoader the class loader to be used for checking
     * @return {@code true} if the class name matches the class loader, {@code false} otherwise
     */
    abstract boolean matches(String className, ClassLoader classLoader);

		/**
     * Checks if a class with the given name is present in the classpath.
     * 
     * @param className the fully qualified name of the class to check
     * @param classLoader the class loader to use for loading the class (optional, defaults to the default class loader)
     * @return true if the class is present, false otherwise
     */
    static boolean isPresent(String className, ClassLoader classLoader) {
			if (classLoader == null) {
				classLoader = ClassUtils.getDefaultClassLoader();
			}
			try {
				resolve(className, classLoader);
				return true;
			}
			catch (Throwable ex) {
				return false;
			}
		}

	}

}

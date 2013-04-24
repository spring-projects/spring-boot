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

package org.springframework.bootstrap.cli.compiler;

import groovy.grape.Grape;
import groovy.lang.Grapes;
import groovy.lang.GroovyClassLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Customizer that allows dependencies to be added during compilation. Delegates to Groovy
 * {@link Grapes} to actually resolve dependencies. This class provides a fluent API for
 * conditionally adding dependencies. For example:
 * {@code dependencies.ifMissing("com.corp.SomeClass").add(group, module, version)}.
 * 
 * @author Phillip Webb
 */
public class DependencyCustomizer {

	private final GroovyClassLoader loader;

	private final List<Map<String, Object>> dependencies;

	/**
	 * Create a new {@link DependencyCustomizer} instance. The {@link #call()} method must
	 * be used to actually resolve dependencies.
	 * @param loader
	 */
	public DependencyCustomizer(GroovyClassLoader loader) {
		this.loader = loader;
		this.dependencies = new ArrayList<Map<String, Object>>();
	}

	/**
	 * Create a new nested {@link DependencyCustomizer}.
	 * @param parent
	 */
	protected DependencyCustomizer(DependencyCustomizer parent) {
		this.loader = parent.loader;
		this.dependencies = parent.dependencies;
	}

	/**
	 * Create a nested {@link DependencyCustomizer} that only applies if the specified
	 * class names are not on the class path.
	 * @param classNames the class names to test
	 * @return a nested {@link DependencyCustomizer}
	 */
	public DependencyCustomizer ifMissingClasses(final String... classNames) {
		return new DependencyCustomizer(this) {
			@Override
			protected boolean canAdd() {
				for (String classname : classNames) {
					try {
						DependencyCustomizer.this.loader.loadClass(classname);
						return false;
					} catch (Exception e) {
					}
				}
				return DependencyCustomizer.this.canAdd();
			}
		};
	}

	/**
	 * Add a single dependencies.
	 * @param group the group ID
	 * @param module the module ID
	 * @param version the version
	 * @return this {@link DependencyCustomizer} for continued use
	 */
	@SuppressWarnings("unchecked")
	public DependencyCustomizer add(String group, String module, String version) {
		if (canAdd()) {
			Map<String, Object> dependency = new HashMap<String, Object>();
			dependency.put("group", group);
			dependency.put("module", module);
			dependency.put("version", version);
			dependency.put("transitive", true);
			return add(dependency);
		}
		return this;
	}

	/**
	 * Add a dependencies.
	 * @param dependencies a map of the dependencies to add.
	 * @return this {@link DependencyCustomizer} for continued use
	 */
	public DependencyCustomizer add(Map<String, Object>... dependencies) {
		this.dependencies.addAll(Arrays.asList(dependencies));
		return this;
	}

	/**
	 * Strategy called to test if dependencies can be added. Subclasses override as
	 * requred.
	 */
	protected boolean canAdd() {
		return true;
	}

	/**
	 * Apply the dependencies.
	 */
	void call() {
		HashMap<String, Object> args = new HashMap<String, Object>();
		args.put("classLoader", this.loader);
		Grape.grab(args, this.dependencies.toArray(new Map[this.dependencies.size()]));
	}
}

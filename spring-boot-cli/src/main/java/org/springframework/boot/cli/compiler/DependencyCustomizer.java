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

package org.springframework.boot.cli.compiler;

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

	private final ArtifactCoordinatesResolver artifactCoordinatesResolver;

	/**
	 * Create a new {@link DependencyCustomizer} instance. The {@link #call()} method must
	 * be used to actually resolve dependencies.
	 * @param loader
	 */
	public DependencyCustomizer(GroovyClassLoader loader,
			ArtifactCoordinatesResolver artifactCoordinatesResolver) {
		this.loader = loader;
		this.artifactCoordinatesResolver = artifactCoordinatesResolver;
		this.dependencies = new ArrayList<Map<String, Object>>();
	}

	/**
	 * Create a new nested {@link DependencyCustomizer}.
	 * @param parent
	 */
	protected DependencyCustomizer(DependencyCustomizer parent) {
		this.loader = parent.loader;
		this.artifactCoordinatesResolver = parent.artifactCoordinatesResolver;
		this.dependencies = parent.dependencies;
	}

	public String getVersion(String artifactId) {
		return getVersion(artifactId, "");

	}

	public String getVersion(String artifactId, String defaultVersion) {
		String version = this.artifactCoordinatesResolver.getVersion(artifactId);
		if (version == null) {
			version = defaultVersion;
		}
		return version;
	}

	/**
	 * Create a nested {@link DependencyCustomizer} that only applies if any of the
	 * specified class names are not on the class path.
	 * @param classNames the class names to test
	 * @return a nested {@link DependencyCustomizer}
	 */
	public DependencyCustomizer ifAnyMissingClasses(final String... classNames) {
		return new DependencyCustomizer(this) {
			@Override
			protected boolean canAdd() {
				for (String classname : classNames) {
					try {
						DependencyCustomizer.this.loader.loadClass(classname);
					}
					catch (Exception ex) {
						return true;
					}
				}
				return DependencyCustomizer.this.canAdd();
			}
		};
	}

	/**
	 * Create a nested {@link DependencyCustomizer} that only applies if all of the
	 * specified class names are not on the class path.
	 * @param classNames the class names to test
	 * @return a nested {@link DependencyCustomizer}
	 */
	public DependencyCustomizer ifAllMissingClasses(final String... classNames) {
		return new DependencyCustomizer(this) {
			@Override
			protected boolean canAdd() {
				for (String classname : classNames) {
					try {
						DependencyCustomizer.this.loader.loadClass(classname);
						return false;
					}
					catch (Exception ex) {
						// swallow exception and continue
					}
				}
				return DependencyCustomizer.this.canAdd();
			}
		};
	}

	/**
	 * Create a nested {@link DependencyCustomizer} that only applies if the specified
	 * paths are on the class path.
	 * @param paths the paths to test
	 * @return a nested {@link DependencyCustomizer}
	 */
	public DependencyCustomizer ifAllResourcesPresent(final String... paths) {
		return new DependencyCustomizer(this) {
			@Override
			protected boolean canAdd() {
				for (String path : paths) {
					try {
						if (DependencyCustomizer.this.loader.getResource(path) == null) {
							return false;
						}
						return true;
					}
					catch (Exception ex) {
						// swallow exception and continue
					}
				}
				return DependencyCustomizer.this.canAdd();
			}
		};
	}

	/**
	 * Create a nested {@link DependencyCustomizer} that only applies at least one of the
	 * specified paths is on the class path.
	 * @param paths the paths to test
	 * @return a nested {@link DependencyCustomizer}
	 */
	public DependencyCustomizer ifAnyResourcesPresent(final String... paths) {
		return new DependencyCustomizer(this) {
			@Override
			protected boolean canAdd() {
				for (String path : paths) {
					try {
						if (DependencyCustomizer.this.loader.getResource(path) != null) {
							return true;
						}
						return false;
					}
					catch (Exception ex) {
						// swallow exception and continue
					}
				}
				return DependencyCustomizer.this.canAdd();
			}
		};
	}

	/**
	 * Create a nested {@link DependencyCustomizer} that only applies the specified one
	 * was not yet added.
	 * @return a nested {@link DependencyCustomizer}
	 */
	public DependencyCustomizer ifNotAdded(final String group, final String module) {
		return new DependencyCustomizer(this) {
			@Override
			protected boolean canAdd() {
				if (DependencyCustomizer.this.contains(group, module)) {
					return false;
				}
				return DependencyCustomizer.this.canAdd();
			}
		};
	}

	/**
	 * @param group the group ID
	 * @param module the module ID
	 * @return true if this module is already in the dependencies
	 */
	protected boolean contains(String group, String module) {
		for (Map<String, Object> dependency : this.dependencies) {
			if (group.equals(dependency.get("group"))
					&& module.equals(dependency.get("module"))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Add a single dependency and all of its dependencies. The group ID and version of
	 * the dependency are resolves using the customizer's
	 * {@link ArtifactCoordinatesResolver}.
	 * @param module The module ID
	 * @return this {@link DependencyCustomizer} for continued use
	 */
	public DependencyCustomizer add(String module) {
		return this.add(this.artifactCoordinatesResolver.getGroupId(module), module,
				this.artifactCoordinatesResolver.getVersion(module), true);
	}

	/**
	 * Add a single dependency and, optionally, all of its dependencies. The group ID and
	 * version of the dependency are resolves using the customizer's
	 * {@link ArtifactCoordinatesResolver}.
	 * @param module The module ID
	 * @param transitive {@code true} if the transitive dependencies should also be added,
	 * otherwise {@code false}.
	 * @return this {@link DependencyCustomizer} for continued use
	 */
	public DependencyCustomizer add(String module, boolean transitive) {
		return this.add(this.artifactCoordinatesResolver.getGroupId(module), module,
				this.artifactCoordinatesResolver.getVersion(module), transitive);
	}

	@SuppressWarnings("unchecked")
	private DependencyCustomizer add(String group, String module, String version,
			boolean transitive) {
		if (canAdd()) {
			Map<String, Object> dependency = new HashMap<String, Object>();
			dependency.put("group", group);
			dependency.put("module", module);
			dependency.put("version", version);
			dependency.put("transitive", transitive);
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
	 * required.
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

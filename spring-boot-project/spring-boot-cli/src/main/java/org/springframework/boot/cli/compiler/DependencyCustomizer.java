/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.cli.compiler;

import groovy.lang.Grab;
import groovy.lang.GroovyClassLoader;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.expr.ConstantExpression;

import org.springframework.boot.cli.compiler.dependencies.ArtifactCoordinatesResolver;
import org.springframework.boot.cli.compiler.grape.DependencyResolutionContext;

/**
 * Customizer that allows dependencies to be added during compilation. Adding a dependency
 * results in a {@link Grab @Grab} annotation being added to the primary {@link ClassNode
 * class} is the {@link ModuleNode module} that's being customized.
 * <p>
 * This class provides a fluent API for conditionally adding dependencies. For example:
 * {@code dependencies.ifMissing("com.corp.SomeClass").add(module)}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.0.0
 */
public class DependencyCustomizer {

	private final GroovyClassLoader loader;

	private final ClassNode classNode;

	private final DependencyResolutionContext dependencyResolutionContext;

	/**
	 * Create a new {@link DependencyCustomizer} instance.
	 * @param loader the current classloader
	 * @param moduleNode the current module
	 * @param dependencyResolutionContext the context for dependency resolution
	 */
	public DependencyCustomizer(GroovyClassLoader loader, ModuleNode moduleNode,
			DependencyResolutionContext dependencyResolutionContext) {
		this.loader = loader;
		this.classNode = moduleNode.getClasses().get(0);
		this.dependencyResolutionContext = dependencyResolutionContext;
	}

	/**
	 * Create a new nested {@link DependencyCustomizer}.
	 * @param parent the parent customizer
	 */
	protected DependencyCustomizer(DependencyCustomizer parent) {
		this.loader = parent.loader;
		this.classNode = parent.classNode;
		this.dependencyResolutionContext = parent.dependencyResolutionContext;
	}

	public String getVersion(String artifactId) {
		return getVersion(artifactId, "");
	}

	public String getVersion(String artifactId, String defaultVersion) {
		String version = this.dependencyResolutionContext.getArtifactCoordinatesResolver().getVersion(artifactId);
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
	public DependencyCustomizer ifAnyMissingClasses(String... classNames) {
		return new DependencyCustomizer(this) {
			@Override
			protected boolean canAdd() {
				for (String className : classNames) {
					try {
						DependencyCustomizer.this.loader.loadClass(className);
					}
					catch (Exception ex) {
						return true;
					}
				}
				return false;
			}
		};
	}

	/**
	 * Create a nested {@link DependencyCustomizer} that only applies if all of the
	 * specified class names are not on the class path.
	 * @param classNames the class names to test
	 * @return a nested {@link DependencyCustomizer}
	 */
	public DependencyCustomizer ifAllMissingClasses(String... classNames) {
		return new DependencyCustomizer(this) {
			@Override
			protected boolean canAdd() {
				for (String className : classNames) {
					try {
						DependencyCustomizer.this.loader.loadClass(className);
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
	public DependencyCustomizer ifAllResourcesPresent(String... paths) {
		return new DependencyCustomizer(this) {
			@Override
			protected boolean canAdd() {
				for (String path : paths) {
					try {
						if (DependencyCustomizer.this.loader.getResource(path) == null) {
							return false;
						}
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
	public DependencyCustomizer ifAnyResourcesPresent(String... paths) {
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
	 * Add dependencies and all of their dependencies. The group ID and version of the
	 * dependencies are resolved from the modules using the customizer's
	 * {@link ArtifactCoordinatesResolver}.
	 * @param modules the module IDs
	 * @return this {@link DependencyCustomizer} for continued use
	 */
	public DependencyCustomizer add(String... modules) {
		for (String module : modules) {
			add(module, null, null, true);
		}
		return this;
	}

	/**
	 * Add a single dependency and, optionally, all of its dependencies. The group ID and
	 * version of the dependency are resolved from the module using the customizer's
	 * {@link ArtifactCoordinatesResolver}.
	 * @param module the module ID
	 * @param transitive {@code true} if the transitive dependencies should also be added,
	 * otherwise {@code false}
	 * @return this {@link DependencyCustomizer} for continued use
	 */
	public DependencyCustomizer add(String module, boolean transitive) {
		return add(module, null, null, transitive);
	}

	/**
	 * Add a single dependency with the specified classifier and type and, optionally, all
	 * of its dependencies. The group ID and version of the dependency are resolved from
	 * the module by using the customizer's {@link ArtifactCoordinatesResolver}.
	 * @param module the module ID
	 * @param classifier the classifier, may be {@code null}
	 * @param type the type, may be {@code null}
	 * @param transitive {@code true} if the transitive dependencies should also be added,
	 * otherwise {@code false}
	 * @return this {@link DependencyCustomizer} for continued use
	 */
	public DependencyCustomizer add(String module, String classifier, String type, boolean transitive) {
		if (canAdd()) {
			ArtifactCoordinatesResolver artifactCoordinatesResolver = this.dependencyResolutionContext
					.getArtifactCoordinatesResolver();
			this.classNode.addAnnotation(createGrabAnnotation(artifactCoordinatesResolver.getGroupId(module),
					artifactCoordinatesResolver.getArtifactId(module), artifactCoordinatesResolver.getVersion(module),
					classifier, type, transitive));
		}
		return this;
	}

	private AnnotationNode createGrabAnnotation(String group, String module, String version, String classifier,
			String type, boolean transitive) {
		AnnotationNode annotationNode = new AnnotationNode(new ClassNode(Grab.class));
		annotationNode.addMember("group", new ConstantExpression(group));
		annotationNode.addMember("module", new ConstantExpression(module));
		annotationNode.addMember("version", new ConstantExpression(version));
		if (classifier != null) {
			annotationNode.addMember("classifier", new ConstantExpression(classifier));
		}
		if (type != null) {
			annotationNode.addMember("type", new ConstantExpression(type));
		}
		annotationNode.addMember("transitive", new ConstantExpression(transitive));
		annotationNode.addMember("initClass", new ConstantExpression(false));
		return annotationNode;
	}

	/**
	 * Strategy called to test if dependencies can be added. Subclasses override as
	 * required. Returns {@code true} by default.
	 * @return {@code true} if dependencies can be added, otherwise {@code false}
	 */
	protected boolean canAdd() {
		return true;
	}

	/**
	 * Returns the {@link DependencyResolutionContext}.
	 * @return the dependency resolution context
	 */
	public DependencyResolutionContext getDependencyResolutionContext() {
		return this.dependencyResolutionContext;
	}

}

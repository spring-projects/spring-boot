/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.gradle.tasks.bundling;

import java.util.Set;

import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.jspecify.annotations.Nullable;

/**
 * A Spring Boot "fat" archive task.
 *
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 * @author Greg Taube
 * @since 2.0.0
 */
public interface BootArchive extends Task {

	/**
	 * Returns the fully-qualified name of the application's main class.
	 * @return the fully-qualified name of the application's main class
	 * @since 2.4.0
	 */
	@Input
	Property<String> getMainClass();

	/**
	 * Adds Ant-style patterns that identify files that must be unpacked from the archive
	 * when it is launched.
	 * @param patterns the patterns
	 */
	void requiresUnpack(String... patterns);

	/**
	 * Adds a spec that identifies files that must be unpacked from the archive when it is
	 * launched.
	 * @param spec the spec
	 */
	void requiresUnpack(Spec<FileTreeElement> spec);

	/**
	 * Returns the classpath that will be included in the archive.
	 * @return the classpath
	 */
	@Optional
	@Classpath
	@Nullable FileCollection getClasspath();

	/**
	 * Adds files to the classpath to include in the archive. The given {@code classpath}
	 * is evaluated as per {@link Project#files(Object...)}.
	 * @param classpath the additions to the classpath
	 */
	void classpath(Object... classpath);

	/**
	 * Sets the classpath to include in the archive. The given {@code classpath} is
	 * evaluated as per {@link Project#files(Object...)}.
	 * @param classpath the classpath
	 * @since 2.0.7
	 */
	void setClasspath(Object classpath);

	/**
	 * Sets the classpath to include in the archive.
	 * @param classpath the classpath
	 * @since 2.0.7
	 */
	void setClasspath(FileCollection classpath);

	/**
	 * Returns the target Java version of the project (e.g. as provided by the
	 * {@code targetCompatibility} build property).
	 * @return the target Java version
	 */
	@Input
	@Optional
	Property<JavaVersion> getTargetJavaVersion();

	/**
	 * Registers the given lazily provided {@code resolvedArtifacts}. They are used to map
	 * from the files in the {@link #getClasspath classpath} to their dependency
	 * coordinates. This overload obtains project coordinates through cross-project
	 * access. Use {@link #resolvedArtifacts(Provider, Provider)} with isolated projects.
	 * @param resolvedArtifacts the lazily provided resolved artifacts
	 * @since 3.0.7
	 */
	void resolvedArtifacts(Provider<Set<ResolvedArtifactResult>> resolvedArtifacts);

	/**
	 * Registers the given lazily provided artifacts and their dependency graph. Project
	 * coordinates are obtained from resolved components without cross-project access. The
	 * artifacts and root component must come from the same configuration; artifacts may
	 * also come from an artifact view of that configuration.
	 * <p>
	 * The default implementation delegates to {@link #resolvedArtifacts(Provider)}.
	 * {@link BootJar} and {@link BootWar} override it to use the dependency graph.
	 * @param resolvedArtifacts the lazily provided resolved artifacts
	 * @param rootComponent the lazily provided root component of the dependency graph
	 * @since 4.2.0
	 */
	default void resolvedArtifacts(Provider<Set<ResolvedArtifactResult>> resolvedArtifacts,
			Provider<ResolvedComponentResult> rootComponent) {
		resolvedArtifacts(resolvedArtifacts);
	}

	/**
	 * Returns whether the JAR tools should be included as a dependency in the layered
	 * archive.
	 * @return whether the JAR tools should be included
	 * @since 3.3.0
	 */
	@Input
	Property<Boolean> getIncludeTools();

}

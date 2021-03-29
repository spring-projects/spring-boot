/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.gradle.tasks.bundling;

import java.io.File;
import java.util.Collections;
import java.util.concurrent.Callable;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileCopyDetails;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.bundling.Jar;

/**
 * A custom {@link Jar} task that produces a Spring Boot executable jar.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author Scott Frederick
 * @author Phillip Webb
 * @since 2.0.0
 */
public class BootJar extends Jar implements BootArchive {

	private static final String LAUNCHER = "org.springframework.boot.loader.JarLauncher";

	private static final String CLASSES_DIRECTORY = "BOOT-INF/classes/";

	private static final String LIB_DIRECTORY = "BOOT-INF/lib/";

	private static final String LAYERS_INDEX = "BOOT-INF/layers.idx";

	private static final String CLASSPATH_INDEX = "BOOT-INF/classpath.idx";

	private final BootArchiveSupport support;

	private final CopySpec bootInfSpec;

	private String mainClassName;

	private FileCollection classpath;

	private LayeredSpec layered;

	/**
	 * Creates a new {@code BootJar} task.
	 */
	public BootJar() {
		this.support = new BootArchiveSupport(LAUNCHER, this::isLibrary, this::resolveZipCompression);
		this.bootInfSpec = getProject().copySpec().into("BOOT-INF");
		configureBootInfSpec(this.bootInfSpec);
		getMainSpec().with(this.bootInfSpec);
	}

	private void configureBootInfSpec(CopySpec bootInfSpec) {
		bootInfSpec.into("classes", fromCallTo(this::classpathDirectories));
		bootInfSpec.into("lib", fromCallTo(this::classpathFiles)).eachFile(this.support::excludeNonZipFiles);
		bootInfSpec.filesMatching("module-info.class",
				(details) -> details.setRelativePath(details.getRelativeSourcePath()));
	}

	private Iterable<File> classpathDirectories() {
		return classpathEntries(File::isDirectory);
	}

	private Iterable<File> classpathFiles() {
		return classpathEntries(File::isFile);
	}

	private Iterable<File> classpathEntries(Spec<File> filter) {
		return (this.classpath != null) ? this.classpath.filter(filter) : Collections.emptyList();
	}

	@Override
	public void copy() {
		this.support.configureManifest(getManifest(), getMainClassName(), CLASSES_DIRECTORY, LIB_DIRECTORY,
				CLASSPATH_INDEX, (this.layered != null) ? LAYERS_INDEX : null);
		super.copy();
	}

	@Override
	protected CopyAction createCopyAction() {
		if (this.layered != null) {
			JavaPluginConvention javaPluginConvention = getProject().getConvention()
					.findPlugin(JavaPluginConvention.class);
			Iterable<SourceSet> sourceSets = (javaPluginConvention != null) ? javaPluginConvention.getSourceSets()
					: Collections.emptySet();
			LayerResolver layerResolver = new LayerResolver(sourceSets, getConfigurations(), this.layered,
					this::isLibrary);
			String layerToolsLocation = this.layered.isIncludeLayerTools() ? LIB_DIRECTORY : null;
			return this.support.createCopyAction(this, layerResolver, layerToolsLocation);
		}
		return this.support.createCopyAction(this);
	}

	/**
	 * Returns the {@link Configuration Configurations} of the project associated with
	 * this task.
	 * @return the configurations
	 * @deprecated since 2.3.5 for removal in 2.5 in favor of
	 * {@link Project#getConfigurations}
	 */
	@Internal
	@Deprecated
	protected Iterable<Configuration> getConfigurations() {
		return getProject().getConfigurations();
	}

	@Override
	public String getMainClassName() {
		if (this.mainClassName == null) {
			String manifestStartClass = (String) getManifest().getAttributes().get("Start-Class");
			if (manifestStartClass != null) {
				setMainClassName(manifestStartClass);
			}
		}
		return this.mainClassName;
	}

	@Override
	public void setMainClassName(String mainClassName) {
		this.mainClassName = mainClassName;
	}

	@Override
	public void requiresUnpack(String... patterns) {
		this.support.requiresUnpack(patterns);
	}

	@Override
	public void requiresUnpack(Spec<FileTreeElement> spec) {
		this.support.requiresUnpack(spec);
	}

	@Override
	public LaunchScriptConfiguration getLaunchScript() {
		return this.support.getLaunchScript();
	}

	@Override
	public void launchScript() {
		enableLaunchScriptIfNecessary();
	}

	@Override
	public void launchScript(Action<LaunchScriptConfiguration> action) {
		action.execute(enableLaunchScriptIfNecessary());
	}

	/**
	 * Returns the spec that describes the layers in a layerd jar.
	 * @return the spec for the layers or {@code null}.
	 * @since 2.3.0
	 */
	@Nested
	@Optional
	public LayeredSpec getLayered() {
		return this.layered;
	}

	/**
	 * Configures the jar to be layered using the default layering.
	 * @since 2.3.0
	 */
	public void layered() {
		enableLayeringIfNecessary();
	}

	/**
	 * Configures the jar to be layered, customizing the layers using the given
	 * {@code action}.
	 * @param action the action to apply
	 * @since 2.3.0
	 */
	public void layered(Action<LayeredSpec> action) {
		action.execute(enableLayeringIfNecessary());
	}

	@Override
	public FileCollection getClasspath() {
		return this.classpath;
	}

	@Override
	public void classpath(Object... classpath) {
		FileCollection existingClasspath = this.classpath;
		this.classpath = getProject().files((existingClasspath != null) ? existingClasspath : Collections.emptyList(),
				classpath);
	}

	@Override
	public void setClasspath(Object classpath) {
		this.classpath = getProject().files(classpath);
	}

	@Override
	public void setClasspath(FileCollection classpath) {
		this.classpath = getProject().files(classpath);
	}

	@Override
	public boolean isExcludeDevtools() {
		return this.support.isExcludeDevtools();
	}

	@Override
	public void setExcludeDevtools(boolean excludeDevtools) {
		this.support.setExcludeDevtools(excludeDevtools);
	}

	/**
	 * Returns a {@code CopySpec} that can be used to add content to the {@code BOOT-INF}
	 * directory of the jar.
	 * @return a {@code CopySpec} for {@code BOOT-INF}
	 * @since 2.0.3
	 */
	@Internal
	public CopySpec getBootInf() {
		CopySpec child = getProject().copySpec();
		this.bootInfSpec.with(child);
		return child;
	}

	/**
	 * Calls the given {@code action} to add content to the {@code BOOT-INF} directory of
	 * the jar.
	 * @param action the {@code Action} to call
	 * @return the {@code CopySpec} for {@code BOOT-INF} that was passed to the
	 * {@code Action}
	 * @since 2.0.3
	 */
	public CopySpec bootInf(Action<CopySpec> action) {
		CopySpec bootInf = getBootInf();
		action.execute(bootInf);
		return bootInf;
	}

	/**
	 * Return the {@link ZipCompression} that should be used when adding the file
	 * represented by the given {@code details} to the jar. By default, any
	 * {@link #isLibrary(FileCopyDetails) library} is {@link ZipCompression#STORED stored}
	 * and all other files are {@link ZipCompression#DEFLATED deflated}.
	 * @param details the file copy details
	 * @return the compression to use
	 */
	protected ZipCompression resolveZipCompression(FileCopyDetails details) {
		return isLibrary(details) ? ZipCompression.STORED : ZipCompression.DEFLATED;
	}

	/**
	 * Return if the {@link FileCopyDetails} are for a library. By default any file in
	 * {@code BOOT-INF/lib} is considered to be a library.
	 * @param details the file copy details
	 * @return {@code true} if the details are for a library
	 * @since 2.3.0
	 */
	protected boolean isLibrary(FileCopyDetails details) {
		String path = details.getRelativePath().getPathString();
		return path.startsWith(LIB_DIRECTORY);
	}

	private LaunchScriptConfiguration enableLaunchScriptIfNecessary() {
		LaunchScriptConfiguration launchScript = this.support.getLaunchScript();
		if (launchScript == null) {
			launchScript = new LaunchScriptConfiguration(this);
			this.support.setLaunchScript(launchScript);
		}
		return launchScript;
	}

	private LayeredSpec enableLayeringIfNecessary() {
		if (this.layered == null) {
			this.layered = new LayeredSpec();
		}
		return this.layered;
	}

	/**
	 * Syntactic sugar that makes {@link CopySpec#into} calls a little easier to read.
	 * @param <T> the result type
	 * @param callable the callable
	 * @return an action to add the callable to the spec
	 */
	private static <T> Action<CopySpec> fromCallTo(Callable<T> callable) {
		return (spec) -> spec.from(callTo(callable));
	}

	/**
	 * Syntactic sugar that makes {@link CopySpec#from} calls a little easier to read.
	 * @param <T> the result type
	 * @param callable the callable
	 * @return the callable
	 */
	private static <T> Callable<T> callTo(Callable<T> callable) {
		return callable;
	}

}

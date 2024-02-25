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

package org.springframework.boot.gradle.tasks.bundling;

import java.io.File;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Function;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileCopyDetails;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.provider.Provider;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.work.DisableCachingByDefault;

import org.springframework.boot.loader.tools.LoaderImplementation;

/**
 * A custom {@link Jar} task that produces a Spring Boot executable jar.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author Scott Frederick
 * @author Phillip Webb
 * @since 2.0.0
 */
@DisableCachingByDefault(because = "Not worth caching")
public abstract class BootJar extends Jar implements BootArchive {

	private static final String LAUNCHER = "org.springframework.boot.loader.launch.JarLauncher";

	private static final String CLASSES_DIRECTORY = "BOOT-INF/classes/";

	private static final String LIB_DIRECTORY = "BOOT-INF/lib/";

	private static final String LAYERS_INDEX = "BOOT-INF/layers.idx";

	private static final String CLASSPATH_INDEX = "BOOT-INF/classpath.idx";

	private final BootArchiveSupport support;

	private final CopySpec bootInfSpec;

	private final LayeredSpec layered;

	private final Provider<String> projectName;

	private final Provider<Object> projectVersion;

	private final ResolvedDependencies resolvedDependencies;

	private FileCollection classpath;

	/**
	 * Creates a new {@code BootJar} task.
	 */
	public BootJar() {
		this.support = new BootArchiveSupport(LAUNCHER, new LibrarySpec(), new ZipCompressionResolver());
		Project project = getProject();
		this.bootInfSpec = project.copySpec().into("BOOT-INF");
		this.layered = project.getObjects().newInstance(LayeredSpec.class);
		configureBootInfSpec(this.bootInfSpec);
		getMainSpec().with(this.bootInfSpec);
		this.projectName = project.provider(project::getName);
		this.projectVersion = project.provider(project::getVersion);
		this.resolvedDependencies = new ResolvedDependencies(project);
	}

	/**
	 * Configures the bootInfSpec CopySpec for the BootJar class.
	 * @param bootInfSpec the CopySpec to be configured
	 */
	private void configureBootInfSpec(CopySpec bootInfSpec) {
		bootInfSpec.into("classes", fromCallTo(this::classpathDirectories));
		bootInfSpec.into("lib", fromCallTo(this::classpathFiles)).eachFile(this.support::excludeNonZipFiles);
		this.support.moveModuleInfoToRoot(bootInfSpec);
		moveMetaInfToRoot(bootInfSpec);
	}

	/**
	 * Returns an iterable of directories in the classpath.
	 * @return an iterable of directories in the classpath
	 */
	private Iterable<File> classpathDirectories() {
		return classpathEntries(File::isDirectory);
	}

	/**
	 * Returns an iterable collection of files in the classpath.
	 * @return an iterable collection of files in the classpath
	 */
	private Iterable<File> classpathFiles() {
		return classpathEntries(File::isFile);
	}

	/**
	 * Returns an iterable of classpath entries that match the given filter.
	 * @param filter the filter to apply on classpath entries
	 * @return an iterable of classpath entries that match the given filter
	 */
	private Iterable<File> classpathEntries(Spec<File> filter) {
		return (this.classpath != null) ? this.classpath.filter(filter) : Collections.emptyList();
	}

	/**
	 * Moves all files in the META-INF directory to the root directory, excluding specific
	 * files and directories.
	 * @param spec the CopySpec object containing the files to be moved
	 */
	private void moveMetaInfToRoot(CopySpec spec) {
		spec.eachFile((file) -> {
			String path = file.getRelativeSourcePath().getPathString();
			if (path.startsWith("META-INF/") && !path.equals("META-INF/aop.xml") && !path.endsWith(".kotlin_module")
					&& !path.startsWith("META-INF/services/")) {
				this.support.moveToRoot(file);
			}
		});
	}

	/**
	 * Sets the resolved artifacts for the BootJar.
	 * @param resolvedArtifacts the resolved artifacts to set
	 */
	@Override
	public void resolvedArtifacts(Provider<Set<ResolvedArtifactResult>> resolvedArtifacts) {
		this.resolvedDependencies.resolvedArtifacts(resolvedArtifacts);
	}

	/**
	 * Returns the resolved dependencies of the BootJar.
	 * @return the resolved dependencies of the BootJar
	 */
	@Nested
	ResolvedDependencies getResolvedDependencies() {
		return this.resolvedDependencies;
	}

	/**
	 * Copies the necessary files and directories for the boot jar. Configures the
	 * manifest with the specified parameters.
	 *
	 * @see BootJar#configureManifest(Manifest, String, String, String, String, String,
	 * int, String, String)
	 * @see BootJar#getManifest()
	 * @see BootJar#getMainClass()
	 * @see BootJar#CLASSES_DIRECTORY
	 * @see BootJar#LIB_DIRECTORY
	 * @see BootJar#CLASSPATH_INDEX
	 * @see BootJar#LAYERS_INDEX
	 * @see BootJar#isLayeredDisabled()
	 * @see BootJar#getTargetJavaVersion()
	 * @see BootJar#projectName
	 * @see BootJar#projectVersion
	 * @see BootJar#copy()
	 */
	@Override
	public void copy() {
		this.support.configureManifest(getManifest(), getMainClass().get(), CLASSES_DIRECTORY, LIB_DIRECTORY,
				CLASSPATH_INDEX, (isLayeredDisabled()) ? null : LAYERS_INDEX,
				this.getTargetJavaVersion().get().getMajorVersion(), this.projectName.get(), this.projectVersion.get());
		super.copy();
	}

	/**
	 * Returns a boolean value indicating whether the layered feature is disabled.
	 * @return {@code true} if the layered feature is disabled, {@code false} otherwise
	 */
	private boolean isLayeredDisabled() {
		return !getLayered().getEnabled().get();
	}

	/**
	 * Creates a copy action for the BootJar.
	 * @return the created CopyAction
	 */
	@Override
	protected CopyAction createCopyAction() {
		LoaderImplementation loaderImplementation = getLoaderImplementation().getOrElse(LoaderImplementation.DEFAULT);
		if (!isLayeredDisabled()) {
			LayerResolver layerResolver = new LayerResolver(this.resolvedDependencies, this.layered, this::isLibrary);
			String layerToolsLocation = this.layered.getIncludeLayerTools().get() ? LIB_DIRECTORY : null;
			return this.support.createCopyAction(this, this.resolvedDependencies, loaderImplementation, true,
					layerResolver, layerToolsLocation);
		}
		return this.support.createCopyAction(this, this.resolvedDependencies, loaderImplementation, true);
	}

	/**
	 * Specifies that certain files matching the given patterns should be unpacked from
	 * the resulting boot jar.
	 * @param patterns the patterns of files to be unpacked
	 */
	@Override
	public void requiresUnpack(String... patterns) {
		this.support.requiresUnpack(patterns);
	}

	/**
	 * Sets the specification for files that require unpacking in the boot jar.
	 * @param spec the specification for files that require unpacking
	 */
	@Override
	public void requiresUnpack(Spec<FileTreeElement> spec) {
		this.support.requiresUnpack(spec);
	}

	/**
	 * Returns the launch script configuration for the BootJar.
	 * @return the launch script configuration
	 */
	@Override
	public LaunchScriptConfiguration getLaunchScript() {
		return this.support.getLaunchScript();
	}

	/**
	 * Launches the script if necessary. This method enables the launch script if it is
	 * not already enabled.
	 */
	@Override
	public void launchScript() {
		enableLaunchScriptIfNecessary();
	}

	/**
	 * Launches a script with the given configuration.
	 * @param action the action to be executed for launching the script
	 * @see LaunchScriptConfiguration
	 */
	@Override
	public void launchScript(Action<LaunchScriptConfiguration> action) {
		action.execute(enableLaunchScriptIfNecessary());
	}

	/**
	 * Returns the spec that describes the layers in a layered jar.
	 * @return the spec for the layers
	 * @since 2.3.0
	 */
	@Nested
	public LayeredSpec getLayered() {
		return this.layered;
	}

	/**
	 * Configures the jar's layering using the given {@code action}.
	 * @param action the action to apply
	 * @since 2.3.0
	 */
	public void layered(Action<LayeredSpec> action) {
		action.execute(this.layered);
	}

	/**
	 * Returns the classpath of the BootJar.
	 * @return the classpath of the BootJar
	 */
	@Override
	public FileCollection getClasspath() {
		return this.classpath;
	}

	/**
	 * Sets the classpath for the BootJar task.
	 * @param classpath The classpath to be set for the BootJar task.
	 */
	@Override
	public void classpath(Object... classpath) {
		FileCollection existingClasspath = this.classpath;
		this.classpath = getProject().files((existingClasspath != null) ? existingClasspath : Collections.emptyList(),
				classpath);
	}

	/**
	 * Sets the classpath for the BootJar.
	 * @param classpath the classpath to be set
	 */
	@Override
	public void setClasspath(Object classpath) {
		this.classpath = getProject().files(classpath);
	}

	/**
	 * Sets the classpath for the BootJar.
	 * @param classpath the classpath to be set
	 */
	@Override
	public void setClasspath(FileCollection classpath) {
		this.classpath = getProject().files(classpath);
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

	/**
	 * Enables the launch script if it is necessary.
	 * @return The launch script configuration.
	 */
	private LaunchScriptConfiguration enableLaunchScriptIfNecessary() {
		LaunchScriptConfiguration launchScript = this.support.getLaunchScript();
		if (launchScript == null) {
			launchScript = new LaunchScriptConfiguration(this);
			this.support.setLaunchScript(launchScript);
		}
		return launchScript;
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

	/**
	 * LibrarySpec class.
	 */
	private final class LibrarySpec implements Spec<FileCopyDetails> {

		/**
		 * Checks if the given file copy details satisfy the condition of being a library.
		 * @param details the file copy details to be checked
		 * @return true if the file copy details represent a library, false otherwise
		 */
		@Override
		public boolean isSatisfiedBy(FileCopyDetails details) {
			return isLibrary(details);
		}

	}

	/**
	 * ZipCompressionResolver class.
	 */
	private final class ZipCompressionResolver implements Function<FileCopyDetails, ZipCompression> {

		/**
		 * Resolves the appropriate ZipCompression for the given FileCopyDetails.
		 * @param details the FileCopyDetails to resolve the ZipCompression for
		 * @return the resolved ZipCompression
		 */
		@Override
		public ZipCompression apply(FileCopyDetails details) {
			return resolveZipCompression(details);
		}

	}

}

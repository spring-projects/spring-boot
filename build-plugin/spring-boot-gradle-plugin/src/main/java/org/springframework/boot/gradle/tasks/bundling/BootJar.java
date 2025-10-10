/*
 * Copyright 2012-present the original author or authors.
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
import org.jspecify.annotations.Nullable;

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

	private @Nullable FileCollection classpath;

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
		getIncludeTools().convention(true);
	}

	private void configureBootInfSpec(CopySpec bootInfSpec) {
		bootInfSpec.into("classes", fromCallTo(this::classpathDirectories));
		bootInfSpec.into("lib", fromCallTo(this::classpathFiles)).eachFile(this.support::excludeNonZipFiles);
		this.support.moveModuleInfoToRoot(bootInfSpec);
		moveMetaInfToRoot(bootInfSpec);
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

	private void moveMetaInfToRoot(CopySpec spec) {
		spec.eachFile((file) -> {
			String path = file.getRelativeSourcePath().getPathString();
			if (path.startsWith("META-INF/") && !path.equals("META-INF/aop.xml") && !path.endsWith(".kotlin_module")
					&& !path.startsWith("META-INF/services/")) {
				this.support.moveToRoot(file);
			}
		});
	}

	@Override
	public void resolvedArtifacts(Provider<Set<ResolvedArtifactResult>> resolvedArtifacts) {
		this.resolvedDependencies.resolvedArtifacts(resolvedArtifacts);
	}

	@Nested
	ResolvedDependencies getResolvedDependencies() {
		return this.resolvedDependencies;
	}

	@Override
	public void copy() {
		this.support.configureManifest(getManifest(), getMainClass().get(), CLASSES_DIRECTORY, LIB_DIRECTORY,
				CLASSPATH_INDEX, (isLayeredDisabled()) ? null : LAYERS_INDEX,
				this.getTargetJavaVersion().get().getMajorVersion(), this.projectName.get(), this.projectVersion.get());
		super.copy();
	}

	private boolean isLayeredDisabled() {
		return !getLayered().getEnabled().get();
	}

	@Override
	protected CopyAction createCopyAction() {
		LayerResolver layerResolver = null;
		if (!isLayeredDisabled()) {
			layerResolver = new LayerResolver(this.resolvedDependencies, this.layered, this::isLibrary);
		}
		String jarmodeToolsLocation = isIncludeJarmodeTools() ? LIB_DIRECTORY : null;
		return this.support.createCopyAction(this, this.resolvedDependencies, true, layerResolver,
				jarmodeToolsLocation);
	}

	private boolean isIncludeJarmodeTools() {
		return Boolean.TRUE.equals(this.getIncludeTools().get());
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
	public @Nullable LaunchScriptConfiguration getLaunchScript() {
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

	@Override
	public @Nullable FileCollection getClasspath() {
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

	private final class LibrarySpec implements Spec<FileCopyDetails> {

		@Override
		public boolean isSatisfiedBy(FileCopyDetails details) {
			return isLibrary(details);
		}

	}

	private final class ZipCompressionResolver implements Function<FileCopyDetails, ZipCompression> {

		@Override
		public ZipCompression apply(FileCopyDetails details) {
			return resolveZipCompression(details);
		}

	}

}

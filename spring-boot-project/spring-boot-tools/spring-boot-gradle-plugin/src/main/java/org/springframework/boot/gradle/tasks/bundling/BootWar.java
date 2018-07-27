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

package org.springframework.boot.gradle.tasks.bundling;

import java.io.File;
import java.util.Collections;
import java.util.concurrent.Callable;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileCopyDetails;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.bundling.War;

/**
 * A custom {@link War} task that produces a Spring Boot executable war.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class BootWar extends War implements BootArchive {

	private final BootArchiveSupport support = new BootArchiveSupport(
			"org.springframework.boot.loader.WarLauncher", this::resolveZipCompression);

	private String mainClassName;

	private FileCollection providedClasspath;

	/**
	 * Creates a new {@code BootWar} task.
	 */
	public BootWar() {
		getWebInf().into("lib-provided",
				(copySpec) -> copySpec.from(
						(Callable<Iterable<File>>) () -> (this.providedClasspath != null)
								? this.providedClasspath : Collections.emptyList()));
	}

	@Override
	public void copy() {
		this.support.configureManifest(this, getMainClassName());
		super.copy();
	}

	@Override
	protected CopyAction createCopyAction() {
		return this.support.createCopyAction(this);
	}

	@Override
	public String getMainClassName() {
		if (this.mainClassName == null) {
			String manifestStartClass = (String) getManifest().getAttributes()
					.get("Start-Class");
			if (manifestStartClass != null) {
				setMainClassName(manifestStartClass);
			}
		}
		return this.mainClassName;
	}

	@Override
	public void setMainClassName(String mainClass) {
		this.mainClassName = mainClass;
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
	 * Returns the provided classpath, the contents of which will be included in the
	 * {@code WEB-INF/lib-provided} directory of the war.
	 * @return the provided classpath
	 */
	@Optional
	public FileCollection getProvidedClasspath() {
		return this.providedClasspath;
	}

	/**
	 * Adds files to the provided classpath to include in the {@code WEB-INF/lib-provided}
	 * directory of the war. The given {@code classpath} are evaluated as per
	 * {@link Project#files(Object...)}.
	 * @param classpath the additions to the classpath
	 */
	public void providedClasspath(Object... classpath) {
		FileCollection existingClasspath = this.providedClasspath;
		this.providedClasspath = getProject().files(
				(existingClasspath != null) ? existingClasspath : Collections.emptyList(),
				classpath);
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
	 * Returns the {@link ZipCompression} that should be used when adding the file
	 * represented by the given {@code details} to the jar.
	 * <p>
	 * By default, any file in {@code WEB-INF/lib/} or {@code WEB-INF/lib-provided/} is
	 * stored and all other files are deflated.
	 * @param details the details
	 * @return the compression to use
	 */
	protected ZipCompression resolveZipCompression(FileCopyDetails details) {
		String relativePath = details.getRelativePath().getPathString();
		if (relativePath.startsWith("WEB-INF/lib/")
				|| relativePath.startsWith("WEB-INF/lib-provided/")) {
			return ZipCompression.STORED;
		}
		return ZipCompression.DEFLATED;
	}

	private LaunchScriptConfiguration enableLaunchScriptIfNecessary() {
		LaunchScriptConfiguration launchScript = this.support.getLaunchScript();
		if (launchScript == null) {
			launchScript = new LaunchScriptConfiguration();
			this.support.setLaunchScript(launchScript);
		}
		return launchScript;
	}

}

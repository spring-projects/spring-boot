/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.gradle.bundling;

import java.io.File;
import java.util.Collections;
import java.util.concurrent.Callable;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.bundling.War;

/**
 * A custom {@link War} task that produces a Spring Boot executable war.
 *
 * @author Andy Wilkinson
 */
public class BootWar extends War implements BootArchive {

	private final BootArchiveSupport support = new BootArchiveSupport(
			"org.springframework.boot.loader.WarLauncher", "WEB-INF/lib/",
			"WEB-INF/lib-provided");

	private String mainClass;

	private FileCollection providedClasspath;

	public BootWar() {
		getWebInf().into("lib-provided", (copySpec) -> {
			copySpec.from((Callable<Iterable<File>>) () -> {
				return this.providedClasspath == null ? Collections.emptyList()
						: this.providedClasspath;
			});
		});
	}

	@Override
	public void copy() {
		this.support.configureManifest(this, getMainClass());
		super.copy();
	}

	@Override
	protected CopyAction createCopyAction() {
		return this.support.createCopyAction(this);
	}

	@Override
	public String getMainClass() {
		return this.mainClass;
	}

	@Override
	public void setMainClass(String mainClass) {
		this.mainClass = mainClass;
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
	public void launchScript(Action<LaunchScriptConfiguration> action) {
		action.execute(getLaunchScript());
	}

	@Optional
	public FileCollection getProvidedClasspath() {
		return this.providedClasspath;
	}

	/**
	 * Adds files to the provided classpath to include in the war. The given
	 * {@code classpath} are evaluated as per {@link Project#files(Object...)}.
	 *
	 * @param classpath the additions to the classpath
	 */
	public void providedClasspath(Object... classpath) {
		FileCollection existingClasspath = this.providedClasspath;
		this.providedClasspath = getProject().files(
				existingClasspath == null ? Collections.emptyList() : existingClasspath,
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

}

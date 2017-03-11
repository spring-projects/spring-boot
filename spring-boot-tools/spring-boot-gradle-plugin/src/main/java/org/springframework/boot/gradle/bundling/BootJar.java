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
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.bundling.Jar;

import org.springframework.boot.gradle.MainClassSupplier;

/**
 * A custom {@link Jar} task that produces a Spring Boot executable jar.
 *
 * @author Andy Wilkinson
 */
public class BootJar extends Jar implements BootArchive {

	private final MainClassSupplier mainClassSupplier = new MainClassSupplier(
			this::getClasspath);

	private BootArchiveSupport support = new BootArchiveSupport(this.mainClassSupplier, "BOOT-INF/lib");

	private FileCollection classpath;

	private String mainClass;

	public BootJar() {
		this.support.setLoaderMainClass("org.springframework.boot.loader.JarLauncher");
		CopySpec bootInf = getRootSpec().addChildBeforeSpec(getMainSpec())
				.into("BOOT-INF");
		bootInf.into("lib", classpathFiles(File::isFile));
		bootInf.into("classes", classpathFiles(File::isDirectory));
	}

	private Action<CopySpec> classpathFiles(Spec<File> filter) {
		return (copySpec) -> {
			copySpec.from((Callable<Iterable<File>>) () -> {
				return this.classpath == null ? Collections.emptyList()
						: this.classpath.filter(filter);
			});
		};
	}

	@Override
	public void copy() {
		this.support.configureManifest(this);
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
		this.mainClassSupplier.setMainClass(mainClass);
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

	@Override
	public FileCollection getClasspath() {
		return this.classpath;
	}

	@Override
	public void classpath(Object... classpath) {
		FileCollection existingClasspath = this.classpath;
		this.classpath = getProject().files(
				existingClasspath == null ? Collections.emptyList() : existingClasspath,
				classpath);
	}

}

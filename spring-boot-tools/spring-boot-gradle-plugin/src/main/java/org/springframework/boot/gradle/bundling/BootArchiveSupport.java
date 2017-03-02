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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.gradle.api.file.FileTreeElement;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.java.archives.Attributes;
import org.gradle.api.specs.Spec;
import org.gradle.api.specs.Specs;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.util.PatternSet;

/**
 * Support class for implementations of {@link BootArchive}.
 *
 * @author Andy Wilkinson
 */
class BootArchiveSupport {

	private final PatternSet requiresUnpack = new PatternSet();

	private final MainClassSupplier mainClassSupplier;

	private final Set<String> storedPathPrefixes;

	private String loaderMainClass;

	private LaunchScriptConfiguration launchScript = new LaunchScriptConfiguration();

	BootArchiveSupport(MainClassSupplier mainClassSupplier,
			String... storedPathPrefixes) {
		this.mainClassSupplier = mainClassSupplier;
		this.storedPathPrefixes = new HashSet<>(Arrays.asList(storedPathPrefixes));
		this.requiresUnpack.include(Specs.satisfyNone());
	}

	void configureManifest(Jar jar) {
		Attributes attributes = jar.getManifest().getAttributes();
		attributes.putIfAbsent("Main-Class", this.loaderMainClass);
		attributes.putIfAbsent("Start-Class", this.mainClassSupplier.get());
	}

	CopyAction createCopyAction(Jar jar) {
		return new BootZipCopyAction(jar.getArchivePath(), this::requiresUnpacking,
				this.launchScript, this.storedPathPrefixes);
	}

	private boolean requiresUnpacking(FileTreeElement fileTreeElement) {
		return this.requiresUnpack.getAsSpec().isSatisfiedBy(fileTreeElement);
	}

	String getLoaderMainClass() {
		return this.loaderMainClass;
	}

	void setLoaderMainClass(String loaderMainClass) {
		this.loaderMainClass = loaderMainClass;
	}

	LaunchScriptConfiguration getLaunchScript() {
		return this.launchScript;
	}

	void setLaunchScript(LaunchScriptConfiguration launchScript) {
		this.launchScript = launchScript;
	}

	void requiresUnpack(String... patterns) {
		this.requiresUnpack.include(patterns);
	}

	void requiresUnpack(Spec<FileTreeElement> spec) {
		this.requiresUnpack.include(spec);
	}

}

/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.buildtoolplugins.otherbuildsystems.examplerepackageimplementation;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.springframework.boot.loader.tools.Library;
import org.springframework.boot.loader.tools.LibraryCallback;
import org.springframework.boot.loader.tools.LibraryScope;
import org.springframework.boot.loader.tools.Repackager;

public class MyBuildTool {

	public void build() throws IOException {
		File sourceJarFile = /**/ null;
		Repackager repackager = new Repackager(sourceJarFile);
		repackager.setBackupSource(false);
		repackager.repackage(this::getLibraries);
	}

	private void getLibraries(LibraryCallback callback) throws IOException {
		// Build system specific implementation, callback for each dependency
		for (File nestedJar : getCompileScopeJars()) {
			callback.library(new Library(nestedJar, LibraryScope.COMPILE));
		}
		// ...
	}

	private List<File> getCompileScopeJars() {
		return /**/ null;
	}

}

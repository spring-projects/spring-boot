/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.buildtoolplugins.otherbuildsystems.examplerepackageimplementation

import org.springframework.boot.loader.tools.Library
import org.springframework.boot.loader.tools.LibraryCallback
import org.springframework.boot.loader.tools.LibraryScope
import org.springframework.boot.loader.tools.Repackager
import java.io.File
import java.io.IOException

class MyBuildTool {

	@Throws(IOException::class)
	fun build() {
		val sourceJarFile: File =  /**/File(".")
		val repackager = Repackager(sourceJarFile)
		repackager.setBackupSource(false)
		repackager.repackage { callback: LibraryCallback -> getLibraries(callback) }
	}

	@Throws(IOException::class)
	private fun getLibraries(callback: LibraryCallback) {
		// Build system specific implementation, callback for each dependency
		for (nestedJar in getCompileScopeJars()) {
			callback.library(Library(nestedJar, LibraryScope.COMPILE))
		}
		// ...
	}

	private fun getCompileScopeJars(): List<File> {
		return  /**/ listOf()
	}

}


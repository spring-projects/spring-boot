/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.bom.bomr;

import java.util.List;

import org.springframework.boot.build.bom.Library;

class LibraryWithVersionOptions {

	private final Library library;

	private final List<VersionOption> versionOptions;

	LibraryWithVersionOptions(Library library, List<VersionOption> versionOptions) {
		this.library = library;
		this.versionOptions = versionOptions;
	}

	Library getLibrary() {
		return this.library;
	}

	List<VersionOption> getVersionOptions() {
		return this.versionOptions;
	}

}

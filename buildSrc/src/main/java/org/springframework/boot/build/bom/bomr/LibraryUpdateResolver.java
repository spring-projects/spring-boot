/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.bom.bomr;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.boot.build.bom.Library;

/**
 * Resolves library updates.
 *
 * @author Moritz Halbritter
 */
public interface LibraryUpdateResolver {

	/**
	 * Finds library updates.
	 * @param librariesToUpgrade libraries to update
	 * @param librariesByName libraries indexed by name
	 * @return library which have updates
	 */
	List<LibraryWithVersionOptions> findLibraryUpdates(Collection<Library> librariesToUpgrade,
			Map<String, Library> librariesByName);

}

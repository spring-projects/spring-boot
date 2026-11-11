/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.bom.bomr;

import java.util.SortedSet;

import org.springframework.boot.build.bom.Library;
import org.springframework.boot.build.bom.bomr.version.DependencyVersion;

/**
 * Resolves the available versions for a module.
 *
 * @author Andy Wilkinson
 */
interface VersionResolver {

	/**
	 * Resolves the available versions for the module identified by the given
	 * {@code groupId} and {@code artifactId}.
	 * @param groupId module's group ID
	 * @param artifactId module's artifact ID
	 * @param library the library to which the module belongs
	 * @return the available versions
	 */
	SortedSet<DependencyVersion> resolveVersions(String groupId, String artifactId, Library library);

}

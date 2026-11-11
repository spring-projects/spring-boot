/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.bom.bomr;

import java.util.Collection;
import java.util.List;

import org.springframework.boot.build.bom.Library;

/**
 * Resolves upgrades for the libraries in a bom.
 *
 * @author Andy Wilkinson
 */
interface UpgradeResolver {

	/**
	 * Resolves the upgrades to be applied to the given {@code libraries}.
	 * @param librariesToUpgrade the libraries to upgrade
	 * @param libraries all libraries
	 * @return the upgrades
	 */
	List<Upgrade> resolveUpgrades(Collection<Library> librariesToUpgrade, Collection<Library> libraries);

}

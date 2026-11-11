/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.bom.bomr;

import org.springframework.boot.build.bom.Library;

/**
 * An upgrade to change a {@link Library} to use a new version.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @param from the library we're upgrading from
 * @param to the library we're upgrading to (may be a SNAPSHOT)
 * @param toRelease the release version of the library we're ultimately upgrading to
 */
record Upgrade(Library from, Library to, Library toRelease) {

	Upgrade(Library from, Library to) {
		this(from, to, to);
	}

}

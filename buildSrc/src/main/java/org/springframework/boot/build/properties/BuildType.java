/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.properties;

import java.util.Locale;

/**
 * The type of build being performed.
 *
 * @author Phillip Webb
 */
public enum BuildType {

	/**
	 * An open source build.
	 */
	OPEN_SOURCE,

	/**
	 * A commercial build.
	 */
	COMMERCIAL;

	public String toIdentifier() {
		return toString().replace("_", "").toLowerCase(Locale.ROOT);
	}

}

/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.tools;

/**
 * A specialization of {@link Layout} that repackages an existing archive by moving its
 * content to a new location.
 *
 * @author Andy Wilkinson
 * @since 1.4.0
 */
public interface RepackagingLayout extends Layout {

	/**
	 * Returns the location to which classes should be moved.
	 * @return the repackaged classes location
	 */
	String getRepackagedClassesLocation();

}

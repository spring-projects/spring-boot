/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.tools;

import java.io.File;

/**
 * Factory interface used to create a {@link Layout}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @since 1.0.0
 */
@FunctionalInterface
public interface LayoutFactory {

	/**
	 * Return a {@link Layout} for the specified source file.
	 * @param source the source file
	 * @return the layout to use for the file
	 */
	Layout getLayout(File source);

}

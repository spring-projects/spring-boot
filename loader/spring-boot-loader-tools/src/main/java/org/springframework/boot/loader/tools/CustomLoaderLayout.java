/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.tools;

import java.io.IOException;

/**
 * Additional interface that can be implemented by {@link Layout Layouts} that write their
 * own loader classes.
 *
 * @author Phillip Webb
 * @since 1.5.0
 */
@FunctionalInterface
public interface CustomLoaderLayout {

	/**
	 * Write the required loader classes into the JAR.
	 * @param writer the writer used to write the classes
	 * @throws IOException if the classes cannot be written
	 */
	void writeLoadedClasses(LoaderClassesWriter writer) throws IOException;

}

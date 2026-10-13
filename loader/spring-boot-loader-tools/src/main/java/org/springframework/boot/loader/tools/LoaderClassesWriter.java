/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.tools;

import java.io.IOException;
import java.io.InputStream;

/**
 * Writer used by {@link CustomLoaderLayout CustomLoaderLayouts} to write classes into a
 * repackaged JAR.
 *
 * @author Phillip Webb
 * @since 1.5.0
 */
public interface LoaderClassesWriter {

	/**
	 * Write the default required spring-boot-loader classes to the JAR.
	 * @throws IOException if the classes cannot be written
	 */
	void writeLoaderClasses() throws IOException;

	/**
	 * Write custom required spring-boot-loader classes to the JAR.
	 * @param loaderJarResourceName the name of the resource containing the loader classes
	 * to be written
	 * @throws IOException if the classes cannot be written
	 */
	void writeLoaderClasses(String loaderJarResourceName) throws IOException;

	/**
	 * Write a single entry to the JAR.
	 * @param name the name of the entry
	 * @param inputStream the input stream content
	 * @throws IOException if the entry cannot be written
	 */
	void writeEntry(String name, InputStream inputStream) throws IOException;

}

/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

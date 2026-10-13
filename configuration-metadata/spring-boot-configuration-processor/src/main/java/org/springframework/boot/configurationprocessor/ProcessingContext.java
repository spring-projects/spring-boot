/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.configurationprocessor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.springframework.boot.configurationprocessor.model.AnnotationReference;
import org.springframework.boot.configurationprocessor.model.Declaration;
import org.springframework.boot.configurationprocessor.model.TypeDeclaration;

/**
 * The facilities that a {@link ConfigurationMetadataGenerator} needs from the tool that
 * processes the source code.
 *
 * @author Areg Iazychian
 * @since 4.2.0
 */
public interface ProcessingContext {

	/**
	 * Return the declaration of the type with the given name.
	 * @param name the canonical name of the type
	 * @return the type or {@code null} if it is not available
	 */
	TypeDeclaration getTypeDeclaration(String name);

	/**
	 * Return the options that have been passed to the processor.
	 * @return the options
	 */
	Map<String, String> getOptions();

	/**
	 * Report an error.
	 * @param message the message
	 * @param declaration the declaration that the error relates to or {@code null}
	 * @param annotation the annotation that the error relates to or {@code null}
	 */
	void error(String message, Declaration declaration, AnnotationReference annotation);

	/**
	 * Report a warning.
	 * @param message the message
	 */
	void warn(String message);

	/**
	 * Open a resource that is available where the metadata is written.
	 * @param location the location of the resource
	 * @return the content of the resource
	 * @throws IOException if the resource cannot be read
	 */
	InputStream openResource(String location) throws IOException;

	/**
	 * Create a resource where the metadata is written.
	 * @param location the location of the resource
	 * @return a stream to write the content of the resource to
	 * @throws IOException if the resource cannot be created
	 */
	OutputStream createResource(String location) throws IOException;

	/**
	 * Open a resource that provides additional metadata.
	 * @param location the location of the resource
	 * @return the content of the resource
	 * @throws IOException if the resource cannot be read
	 */
	InputStream openAdditionalMetadata(String location) throws IOException;

	/**
	 * Open a resource of the classpath that the source code is compiled against.
	 * @param location the location of the resource
	 * @return the content of the resource
	 * @throws IOException if the resource cannot be read
	 */
	InputStream openClasspathResource(String location) throws IOException;

}

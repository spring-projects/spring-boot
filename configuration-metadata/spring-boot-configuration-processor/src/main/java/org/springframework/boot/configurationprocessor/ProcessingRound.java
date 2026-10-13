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

import java.util.Collection;

import org.springframework.boot.configurationprocessor.model.Declaration;
import org.springframework.boot.configurationprocessor.model.TypeDeclaration;

/**
 * The declarations that a {@link ConfigurationMetadataGenerator} has to process in a
 * round of processing.
 *
 * @author Areg Iazychian
 * @since 4.2.0
 */
public interface ProcessingRound {

	/**
	 * Return the types and the methods that are annotated with the given annotation.
	 * @param annotation the canonical name of the annotation
	 * @return the annotated declarations
	 */
	Collection<? extends Declaration> getDeclarationsAnnotatedWith(String annotation);

	/**
	 * Return the top-level types that are processed in this round.
	 * @return the root types
	 */
	Collection<? extends TypeDeclaration> getRootTypes();

}

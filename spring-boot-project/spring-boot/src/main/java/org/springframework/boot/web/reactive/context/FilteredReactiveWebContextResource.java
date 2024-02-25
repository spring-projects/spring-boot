/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.web.reactive.context;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

/**
 * Resource implementation that replaces the
 * {@link org.springframework.web.context.support.ServletContextResource} in a reactive
 * web application.
 * <p>
 * {@link #exists()} always returns {@code false} in order to avoid exposing the whole
 * classpath in a non-servlet environment.
 *
 * @author Brian Clozel
 */
class FilteredReactiveWebContextResource extends AbstractResource {

	private final String path;

	/**
     * Constructs a new FilteredReactiveWebContextResource with the specified path.
     *
     * @param path the path of the resource
     */
    FilteredReactiveWebContextResource(String path) {
		this.path = path;
	}

	/**
     * Returns a boolean value indicating whether the resource exists.
     *
     * @return {@code true} if the resource exists, {@code false} otherwise
     */
    @Override
	public boolean exists() {
		return false;
	}

	/**
     * Creates a new resource by resolving the given relative path against the current resource's path.
     * 
     * @param relativePath the relative path to resolve against the current resource's path
     * @return the newly created resource
     * @throws IOException if an I/O error occurs while creating the resource
     */
    @Override
	public Resource createRelative(String relativePath) throws IOException {
		String pathToUse = StringUtils.applyRelativePath(this.path, relativePath);
		return new FilteredReactiveWebContextResource(pathToUse);
	}

	/**
     * Returns the description of the ReactiveWebContext resource.
     * 
     * @return the description of the ReactiveWebContext resource
     */
    @Override
	public String getDescription() {
		return "ReactiveWebContext resource [" + this.path + "]";
	}

	/**
     * Returns an input stream for reading the content of this resource.
     *
     * @return an input stream for reading the content of this resource
     * @throws IOException if an I/O error occurs while opening the input stream
     * @throws FileNotFoundException if the resource does not exist
     */
    @Override
	public InputStream getInputStream() throws IOException {
		throw new FileNotFoundException(getDescription() + " cannot be opened because it does not exist");
	}

}

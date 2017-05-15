/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;

/**
 * Subclass of {@link AnnotationConfigApplicationContext}, suitable for reactive web
 * environments.
 *
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @since 2.0.0
 */
public class GenericReactiveWebApplicationContext extends
		AnnotationConfigApplicationContext implements ReactiveWebApplicationContext {

	private String namespace;

	private final NonExistentResource nonExistentResource = new NonExistentResource();

	public GenericReactiveWebApplicationContext() {
		super();
	}

	public GenericReactiveWebApplicationContext(Class<?>[] annotatedClasses) {
		super(annotatedClasses);
	}

	@Override
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	@Override
	public String getNamespace() {
		return this.namespace;
	}

	@Override
	protected Resource getResourceByPath(String path) {
		// No ServletContext is available
		if (path.startsWith("/")) {
			return this.nonExistentResource;
		}
		else {
			return super.getResourceByPath(path);
		}
	}

	/**
	 * Resource implementation that replaces the
	 * {@link org.springframework.web.context.support.ServletContextResource}
	 * in a reactive web application.
	 *
	 * <p>{@link #exists()} always returns null in order to avoid exposing
	 * the whole classpath in a non-servlet environment.
	 */
	class NonExistentResource extends AbstractResource {

		@Override
		public boolean exists() {
			return false;
		}

		@Override
		public Resource createRelative(String relativePath) throws IOException {
			return this;
		}

		@Override
		public String getDescription() {
			return "NonExistentResource";
		}

		@Override
		public InputStream getInputStream() throws IOException {
			throw new FileNotFoundException(this.getDescription() + " cannot be opened because it does not exist");
		}
	}
}

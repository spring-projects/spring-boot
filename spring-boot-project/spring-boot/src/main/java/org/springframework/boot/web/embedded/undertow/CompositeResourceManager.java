/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.web.embedded.undertow;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import io.undertow.UndertowMessages;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceChangeListener;
import io.undertow.server.handlers.resource.ResourceManager;

/**
 * A {@link ResourceManager} that delegates to multiple {@code ResourceManager} instances.
 *
 * @author Andy Wilkinson
 */
class CompositeResourceManager implements ResourceManager {

	private final List<ResourceManager> resourceManagers;

	/**
	 * Constructs a new CompositeResourceManager with the given resource managers.
	 * @param resourceManagers the resource managers to be included in the composite
	 * resource manager
	 */
	CompositeResourceManager(ResourceManager... resourceManagers) {
		this.resourceManagers = Arrays.asList(resourceManagers);
	}

	/**
	 * Closes all the resource managers in the composite resource manager.
	 * @throws IOException if an I/O error occurs while closing the resource managers
	 */
	@Override
	public void close() throws IOException {
		for (ResourceManager resourceManager : this.resourceManagers) {
			resourceManager.close();
		}
	}

	/**
	 * Retrieves a resource from the composite resource managers.
	 * @param path the path of the resource to retrieve
	 * @return the resource if found, null otherwise
	 * @throws IOException if an I/O error occurs while retrieving the resource
	 */
	@Override
	public Resource getResource(String path) throws IOException {
		for (ResourceManager resourceManager : this.resourceManagers) {
			Resource resource = resourceManager.getResource(path);
			if (resource != null) {
				return resource;
			}
		}
		return null;
	}

	/**
	 * Returns whether the CompositeResourceManager supports resource change listeners.
	 * @return {@code false} as the CompositeResourceManager does not support resource
	 * change listeners.
	 */
	@Override
	public boolean isResourceChangeListenerSupported() {
		return false;
	}

	/**
	 * Registers a resource change listener.
	 * @param listener the resource change listener to register
	 * @throws UnsupportedOperationException if resource change listener is not supported
	 */
	@Override
	public void registerResourceChangeListener(ResourceChangeListener listener) {
		throw UndertowMessages.MESSAGES.resourceChangeListenerNotSupported();
	}

	/**
	 * Removes the specified resource change listener from this CompositeResourceManager.
	 * @param listener the resource change listener to be removed
	 * @throws UnsupportedOperationException if resource change listener is not supported
	 */
	@Override
	public void removeResourceChangeListener(ResourceChangeListener listener) {
		throw UndertowMessages.MESSAGES.resourceChangeListenerNotSupported();
	}

}

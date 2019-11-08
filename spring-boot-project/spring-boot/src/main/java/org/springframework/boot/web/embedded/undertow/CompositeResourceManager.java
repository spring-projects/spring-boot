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

	CompositeResourceManager(ResourceManager... resourceManagers) {
		this.resourceManagers = Arrays.asList(resourceManagers);
	}

	@Override
	public void close() throws IOException {
		for (ResourceManager resourceManager : this.resourceManagers) {
			resourceManager.close();
		}
	}

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

	@Override
	public boolean isResourceChangeListenerSupported() {
		return false;
	}

	@Override
	public void registerResourceChangeListener(ResourceChangeListener listener) {
		throw UndertowMessages.MESSAGES.resourceChangeListenerNotSupported();
	}

	@Override
	public void removeResourceChangeListener(ResourceChangeListener listener) {
		throw UndertowMessages.MESSAGES.resourceChangeListenerNotSupported();
	}

}

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

import java.io.File;
import java.io.IOException;
import java.net.URL;

import io.undertow.UndertowMessages;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceChangeListener;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.server.handlers.resource.URLResource;

import org.springframework.util.StringUtils;

/**
 * {@link ResourceManager} for JAR resources.
 *
 * @author Ivan Sopov
 * @author Andy Wilkinson
 */
class JarResourceManager implements ResourceManager {

	private final String jarPath;

	JarResourceManager(File jarFile) {
		this(jarFile.getAbsolutePath());
	}

	JarResourceManager(String jarPath) {
		this.jarPath = jarPath;
	}

	@Override
	public Resource getResource(String path) throws IOException {
		URL url = new URL("jar:file:" + this.jarPath + "!" + (path.startsWith("/") ? path : "/" + path));
		URLResource resource = new URLResource(url, path);
		if (StringUtils.hasText(path) && !"/".equals(path) && resource.getContentLength() < 0) {
			return null;
		}
		return resource;
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

	@Override
	public void close() throws IOException {

	}

}

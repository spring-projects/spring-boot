/*
 * Copyright 2012-2021 the original author or authors.
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
import java.net.MalformedURLException;
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

	/**
     * Constructs a new JarResourceManager with the specified jar file.
     * 
     * @param jarFile the jar file to be used as the resource manager
     * @throws IllegalArgumentException if the jar file path is malformed
     */
    JarResourceManager(File jarFile) {
		try {
			this.jarPath = jarFile.getAbsoluteFile().toURI().toURL().toString();
		}
		catch (MalformedURLException ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	/**
     * Retrieves a resource from the specified path within the JAR file.
     * 
     * @param path the path of the resource to retrieve
     * @return the resource at the specified path, or null if it does not exist
     * @throws IOException if an I/O error occurs while retrieving the resource
     */
    @Override
	public Resource getResource(String path) throws IOException {
		URL url = new URL("jar:" + this.jarPath + "!" + (path.startsWith("/") ? path : "/" + path));
		URLResource resource = new URLResource(url, path);
		if (StringUtils.hasText(path) && !"/".equals(path) && resource.getContentLength() < 0) {
			return null;
		}
		return resource;
	}

	/**
     * Returns whether the JarResourceManager supports resource change listeners.
     * 
     * @return {@code false} if resource change listeners are not supported, {@code true} otherwise.
     */
    @Override
	public boolean isResourceChangeListenerSupported() {
		return false;
	}

	/**
     * Registers a resource change listener.
     * 
     * @param listener the resource change listener to register
     * @throws UnsupportedOperationException if resource change listener is not supported
     */
    @Override
	public void registerResourceChangeListener(ResourceChangeListener listener) {
		throw UndertowMessages.MESSAGES.resourceChangeListenerNotSupported();

	}

	/**
     * Removes the specified resource change listener from this JarResourceManager.
     * 
     * @param listener the resource change listener to be removed
     * @throws UnsupportedOperationException if resource change listeners are not supported by this JarResourceManager
     */
    @Override
	public void removeResourceChangeListener(ResourceChangeListener listener) {
		throw UndertowMessages.MESSAGES.resourceChangeListenerNotSupported();
	}

	/**
     * Closes the resource manager.
     *
     * @throws IOException if an I/O error occurs while closing the resource manager.
     */
    @Override
	public void close() throws IOException {

	}

}

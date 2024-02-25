/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.web.embedded.tomcat;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResource;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.WebResourceSet;
import org.apache.catalina.webresources.AbstractSingleArchiveResourceSet;
import org.apache.catalina.webresources.JarResource;

import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;

/**
 * A {@link WebResourceSet} for a resource in a nested JAR.
 *
 * @author Phillip Webb
 */
class NestedJarResourceSet extends AbstractSingleArchiveResourceSet {

	private static final Name MULTI_RELEASE = new Name("Multi-Release");

	private final URL url;

	private JarFile archive = null;

	private long archiveUseCount = 0;

	private boolean useCaches;

	private volatile Boolean multiRelease;

	/**
     * Constructs a new NestedJarResourceSet with the specified URL, WebResourceRoot, webAppMount, and internalPath.
     * 
     * @param url the URL of the nested JAR file
     * @param root the WebResourceRoot to which this resource set belongs
     * @param webAppMount the mount point of the resource set within the web application
     * @param internalPath the internal path within the nested JAR file
     * @throws IllegalArgumentException if any of the parameters are invalid
     */
    NestedJarResourceSet(URL url, WebResourceRoot root, String webAppMount, String internalPath)
			throws IllegalArgumentException {
		this.url = url;
		setRoot(root);
		setWebAppMount(webAppMount);
		setInternalPath(internalPath);
		setStaticOnly(true);
		if (getRoot().getState().isAvailable()) {
			try {
				start();
			}
			catch (LifecycleException ex) {
				throw new IllegalStateException(ex);
			}
		}
	}

	/**
     * Creates a new WebResource object for the given JarEntry, webAppPath, and Manifest.
     * 
     * @param jarEntry The JarEntry object representing the entry in the JAR file.
     * @param webAppPath The path of the resource within the web application.
     * @param manifest The Manifest object associated with the JAR file.
     * @return A new WebResource object representing the resource.
     */
    @Override
	protected WebResource createArchiveResource(JarEntry jarEntry, String webAppPath, Manifest manifest) {
		return new JarResource(this, webAppPath, getBaseUrlString(), jarEntry);
	}

	/**
     * Initializes the internal state of the NestedJarResourceSet.
     * This method is called during the initialization phase of the resource set's lifecycle.
     * It establishes a connection to the JAR file and sets the manifest and base URL properties.
     * If the connection does not use caches, the JAR file is closed after the necessary information is retrieved.
     * 
     * @throws LifecycleException if an error occurs during the initialization process
     */
    @Override
	protected void initInternal() throws LifecycleException {
		try {
			JarURLConnection connection = connect();
			try {
				setManifest(connection.getManifest());
				setBaseUrl(connection.getJarFileURL());
			}
			finally {
				if (!connection.getUseCaches()) {
					connection.getJarFile().close();
				}
			}
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
     * Opens the JAR file associated with this NestedJarResourceSet.
     * 
     * @return the opened JarFile object
     * @throws IOException if an I/O error occurs while opening the JAR file
     */
    @Override
	protected JarFile openJarFile() throws IOException {
		synchronized (this.archiveLock) {
			if (this.archive == null) {
				JarURLConnection connection = connect();
				this.useCaches = connection.getUseCaches();
				this.archive = connection.getJarFile();
			}
			this.archiveUseCount++;
			return this.archive;
		}
	}

	/**
     * Decreases the usage count of the JAR file and closes it.
     * This method is synchronized to ensure thread safety.
     * 
     * @throws IllegalStateException if the JAR file is already closed
     */
    @Override
	protected void closeJarFile() {
		synchronized (this.archiveLock) {
			this.archiveUseCount--;
		}
	}

	/**
     * Returns whether this NestedJarResourceSet is a multi-release JAR.
     * 
     * @return true if this NestedJarResourceSet is a multi-release JAR, false otherwise
     */
    @Override
	protected boolean isMultiRelease() {
		if (this.multiRelease == null) {
			synchronized (this.archiveLock) {
				if (this.multiRelease == null) {
					// JarFile.isMultiRelease() is final so we must go to the manifest
					Manifest manifest = getManifest();
					Attributes attributes = (manifest != null) ? manifest.getMainAttributes() : null;
					this.multiRelease = (attributes != null) && attributes.containsKey(MULTI_RELEASE);
				}
			}
		}
		return this.multiRelease.booleanValue();
	}

	/**
     * Performs garbage collection on the NestedJarResourceSet.
     * This method is responsible for closing the archive if it is not being used and the useCaches flag is set to false.
     * It also resets the archive and archiveEntries variables to null.
     * 
     * @throws IOException if an I/O error occurs while closing the archive
     */
    @Override
	public void gc() {
		synchronized (this.archiveLock) {
			if (this.archive != null && this.archiveUseCount == 0) {
				try {
					if (!this.useCaches) {
						this.archive.close();
					}
				}
				catch (IOException ex) {
					// Ignore
				}
				this.archive = null;
				this.archiveEntries = null;
			}
		}
	}

	/**
     * Connects to the URL and returns a JarURLConnection.
     * 
     * @return the JarURLConnection
     * @throws IOException if an I/O error occurs while connecting to the URL
     * @throws IllegalStateException if the URL does not return a JarURLConnection
     */
    private JarURLConnection connect() throws IOException {
		URLConnection connection = this.url.openConnection();
		ResourceUtils.useCachesIfNecessary(connection);
		Assert.state(connection instanceof JarURLConnection,
				() -> "URL '%s' did not return a JAR connection".formatted(this.url));
		connection.connect();
		return (JarURLConnection) connection;
	}

}

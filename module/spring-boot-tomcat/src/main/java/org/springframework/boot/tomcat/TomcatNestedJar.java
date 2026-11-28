/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.tomcat;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.apache.tomcat.Jar;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * {@link Jar} implementation that is optimized for nested JAR URLs.
 *
 * @author Manthan Sharma
 * @author Phillip Webb
 */
class TomcatNestedJar implements Jar {

	private final JarFile jarFile;

	private final URL jarFileURL;

	private final boolean multiRelease;

	private @Nullable Enumeration<JarEntry> entries;

	private @Nullable Set<String> entryNamesSeen;

	private @Nullable JarEntry entry;

	TomcatNestedJar(URL url) throws IOException {
		URL jarUrl = asJarUrl(url);
		URLConnection connection = jarUrl.openConnection();
		connection.setUseCaches(false);
		Assert.state(connection instanceof JarURLConnection,
				() -> "URL '%s' did not return a JAR connection".formatted(jarUrl));
		JarURLConnection jarConnection = (JarURLConnection) connection;
		this.jarFile = jarConnection.getJarFile();
		this.jarFileURL = jarConnection.getJarFileURL();
		this.multiRelease = isMultiRelease(this.jarFile);
	}

	TomcatNestedJar(JarFile jarFile, URL jarFileURL) {
		this.jarFile = jarFile;
		this.jarFileURL = jarFileURL;
		this.multiRelease = isMultiRelease(jarFile);
	}

	private static URL asJarUrl(URL url) throws MalformedURLException {
		String spec = url.toExternalForm();
		if ("nested".equalsIgnoreCase(url.getProtocol())) {
			spec = "jar:" + spec + "!/";
		}
		else if ("jar".equalsIgnoreCase(url.getProtocol()) && !spec.endsWith("!/")) {
			spec = spec + "!/";
		}
		return new URL(spec);
	}

	private boolean isMultiRelease(JarFile jarFile) {
		try {
			Manifest manifest = jarFile.getManifest();
			Attributes attributes = (manifest != null) ? manifest.getMainAttributes() : null;
			return (attributes != null) && attributes.containsKey(Attributes.Name.MULTI_RELEASE);
		}
		catch (IOException ex) {
			return false;
		}
	}

	@Override
	public URL getJarFileURL() {
		return this.jarFileURL;
	}

	@Override
	public @Nullable InputStream getInputStream(String name) throws IOException {
		ZipEntry entry = this.jarFile.getEntry(name);
		return (entry != null) ? this.jarFile.getInputStream(entry) : null;
	}

	@Override
	public long getLastModified(String name) throws IOException {
		ZipEntry entry = this.jarFile.getEntry(name);
		return (entry != null) ? entry.getTime() : -1;
	}

	@Override
	public boolean exists(String name) throws IOException {
		return this.jarFile.getEntry(name) != null;
	}

	@Override
	public String getURL(String entry) {
		return "jar:" + getJarFileURL().toExternalForm() + "!/" + entry;
	}

	@Override
	public void close() {
		try {
			this.jarFile.close();
		}
		catch (IOException ex) {
			// Ignore
		}
	}

	@Override
	public void nextEntry() {
		if (this.entries == null) {
			this.entries = this.jarFile.entries();
			if (this.multiRelease) {
				this.entryNamesSeen = new HashSet<>();
			}
		}
		if (this.multiRelease) {
			String name;
			while (true) {
				if (this.entries.hasMoreElements()) {
					this.entry = this.entries.nextElement();
					name = this.entry.getName();
					if (name.startsWith("META-INF/versions/")) {
						int i = name.indexOf('/', 18);
						if (i == -1) {
							continue;
						}
						name = name.substring(i + 1);
					}
					Set<String> entryNamesSeen = this.entryNamesSeen;
					if (name.isEmpty() || (entryNamesSeen != null && !entryNamesSeen.add(name))) {
						continue;
					}
					this.entry = this.jarFile.getJarEntry(this.entry.getName());
				}
				else {
					this.entry = null;
				}
				break;
			}
		}
		else {
			this.entry = this.entries.hasMoreElements() ? this.entries.nextElement() : null;
		}
	}

	@Override
	public @Nullable String getEntryName() {
		return (this.entry != null) ? this.entry.getName() : null;
	}

	@Override
	public @Nullable InputStream getEntryInputStream() throws IOException {
		return (this.entry != null) ? this.jarFile.getInputStream(this.entry) : null;
	}

	@Override
	public @Nullable Manifest getManifest() throws IOException {
		return this.jarFile.getManifest();
	}

	@Override
	public void reset() throws IOException {
		this.entries = null;
		this.entryNamesSeen = null;
		this.entry = null;
	}

}

/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.tomcat;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Deque;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.Jar;
import org.apache.tomcat.JarScanType;
import org.apache.tomcat.JarScannerCallback;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.jspecify.annotations.Nullable;

import org.springframework.util.StringUtils;

/**
 * {@link StandardJarScanner} that can handle nested URLs directly.
 *
 * @author Manthan Sharma
 * @author Phillip Webb
 * @since 4.0.0
 */
public class TomcatJarScanner extends StandardJarScanner {

	private static final Log logger = LogFactory.getLog(TomcatJarScanner.class);

	@Override
	protected void process(JarScanType scanType, JarScannerCallback callback, URL url, @Nullable String webappPath,
			boolean isWebapp, @Nullable Deque<URL> classPathUrlsToProcess) throws IOException {
		if (isNested(url)) {
			processNested(callback, url, webappPath, isWebapp, classPathUrlsToProcess);
		}
		else {
			super.process(scanType, callback, url, webappPath, isWebapp, classPathUrlsToProcess);
		}
	}

	private boolean isNested(URL url) {
		String protocol = url.getProtocol();
		String file = url.getFile();
		return "nested".equalsIgnoreCase(protocol)
				|| ("jar".equalsIgnoreCase(protocol) && file != null && file.startsWith("nested:"));
	}

	private void processNested(JarScannerCallback callback, URL url, @Nullable String webappPath, boolean isWebapp,
			@Nullable Deque<URL> classPathUrlsToProcess) throws IOException {
		try (Jar jar = createNestedJar(url)) {
			if (isScanManifest()) {
				processManifest(jar, isWebapp, classPathUrlsToProcess);
			}
			callback.scan(jar, webappPath, isWebapp);
		}
	}

	protected Jar createNestedJar(URL url) throws IOException {
		return new TomcatNestedJar(url);
	}

	private void processManifest(Jar jar, boolean isWebapp, @Nullable Deque<URL> classPathUrlsToProcess) {
		if (isWebapp || classPathUrlsToProcess == null) {
			return;
		}
		try {
			Manifest manifest = jar.getManifest();
			if (manifest == null) {
				return;
			}
			Attributes attributes = manifest.getMainAttributes();
			String classPathAttribute = attributes.getValue("Class-Path");
			if (classPathAttribute == null) {
				return;
			}
			String[] classPathEntries = StringUtils.delimitedListToStringArray(classPathAttribute, " ");
			for (String classPathEntry : classPathEntries) {
				classPathEntry = classPathEntry.trim();
				if (classPathEntry.isEmpty()) {
					continue;
				}
				URL jarURL = jar.getJarFileURL();
				try {
					URI jarURI = jarURL.toURI();
					URI classPathEntryURI = jarURI.resolve(classPathEntry);
					classPathUrlsToProcess.add(classPathEntryURI.toURL());
				}
				catch (Exception ex) {
					if (logger.isDebugEnabled()) {
						logger.debug(
								"Failed to resolve Class-Path entry [%s] from [%s]".formatted(classPathEntry, jarURL),
								ex);
					}
				}
			}
		}
		catch (IOException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Failed to read manifest from [%s]".formatted(jar.getJarFileURL()), ex);
			}
		}
	}

}

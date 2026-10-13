/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ApplicationLauncher} that launches a remote application with its classes in a
 * jar file.
 *
 * @author Andy Wilkinson
 */
public class JarFileRemoteApplicationLauncher extends RemoteApplicationLauncher {

	public JarFileRemoteApplicationLauncher(Directories directories) {
		super(directories);
	}

	@Override
	protected String createApplicationClassPath() throws Exception {
		File appDirectory = getDirectories().getAppDirectory();
		copyApplicationTo(appDirectory);
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		File appJar = new File(appDirectory, "app.jar");
		JarOutputStream output = new JarOutputStream(new FileOutputStream(appJar), manifest);
		addToJar(output, appDirectory, appDirectory);
		output.close();
		List<String> entries = new ArrayList<>();
		entries.add(appJar.getAbsolutePath());
		entries.addAll(getDependencyJarPaths());
		return StringUtils.collectionToDelimitedString(entries, File.pathSeparator);
	}

	private void addToJar(JarOutputStream output, File root, File current) throws IOException {
		for (File file : current.listFiles()) {
			if (file.isDirectory()) {
				addToJar(output, root, file);
			}
			output.putNextEntry(new ZipEntry(
					file.getAbsolutePath().substring(root.getAbsolutePath().length() + 1).replace("\\", "/")
							+ (file.isDirectory() ? "/" : "")));
			if (file.isFile()) {
				try (FileInputStream input = new FileInputStream(file)) {
					StreamUtils.copy(input, output);
				}
			}
			output.closeEntry();
		}
	}

	@Override
	public String toString() {
		return "jar file remote";
	}

}

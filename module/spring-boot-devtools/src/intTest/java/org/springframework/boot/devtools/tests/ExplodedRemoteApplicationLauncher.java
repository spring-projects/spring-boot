/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.springframework.util.StringUtils;

/**
 * {@link ApplicationLauncher} that launches a remote application with its classes
 * available directly on the file system.
 *
 * @author Andy Wilkinson
 */
public class ExplodedRemoteApplicationLauncher extends RemoteApplicationLauncher {

	public ExplodedRemoteApplicationLauncher(Directories directories) {
		super(directories);
	}

	@Override
	protected String createApplicationClassPath() throws Exception {
		File appDirectory = getDirectories().getAppDirectory();
		copyApplicationTo(appDirectory);
		List<String> entries = new ArrayList<>();
		entries.add(appDirectory.getAbsolutePath());
		entries.addAll(getDependencyJarPaths());
		return StringUtils.collectionToDelimitedString(entries, File.pathSeparator);
	}

	@Override
	public String toString() {
		return "exploded remote";
	}

}

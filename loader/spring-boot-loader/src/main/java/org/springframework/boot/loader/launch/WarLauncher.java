/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.launch;

/**
 * {@link Launcher} for WAR based archives. This launcher for standard WAR archives.
 * Supports dependencies in {@code WEB-INF/lib} as well as {@code WEB-INF/lib-provided},
 * classes are loaded from {@code WEB-INF/classes}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @since 3.2.0
 */
public class WarLauncher extends ExecutableArchiveLauncher {

	public WarLauncher() throws Exception {
	}

	protected WarLauncher(Archive archive) throws Exception {
		super(archive);
	}

	@Override
	protected String getEntryPathPrefix() {
		return "WEB-INF/";
	}

	@Override
	protected boolean isLibraryFileOrClassesDirectory(Archive.Entry entry) {
		String name = entry.name();
		if (entry.isDirectory()) {
			return name.equals("WEB-INF/classes/");
		}
		return name.startsWith("WEB-INF/lib/") || name.startsWith("WEB-INF/lib-provided/");
	}

	public static void main(String[] args) throws Exception {
		new WarLauncher().launch(args);
	}

}

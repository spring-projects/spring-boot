/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.launch;

/**
 * {@link Launcher} for JAR based archives. This launcher assumes that dependency jars are
 * included inside a {@code /BOOT-INF/lib} directory and that application classes are
 * included inside a {@code /BOOT-INF/classes} directory.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author Scott Frederick
 * @since 3.2.0
 */
public class JarLauncher extends ExecutableArchiveLauncher {

	public JarLauncher() throws Exception {
	}

	protected JarLauncher(Archive archive) throws Exception {
		super(archive);
	}

	public static void main(String[] args) throws Exception {
		new JarLauncher().launch(args);
	}

}

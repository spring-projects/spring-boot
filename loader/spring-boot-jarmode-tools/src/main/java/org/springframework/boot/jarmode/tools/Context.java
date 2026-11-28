/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jarmode.tools;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Locale;
import java.util.jar.JarFile;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.loader.launch.Launcher;
import org.springframework.util.Assert;

/**
 * Context for use by commands.
 *
 * @author Phillip Webb
 * @author Xinwei Zhang
 */
class Context {

	private final File archiveFile;

	private final File workingDir;

	private final @Nullable String relativeDir;

	/**
	 * Create a new {@link Context} instance.
	 */
	Context() {
		this(getSourceArchiveFile(), Paths.get(".").toAbsolutePath().normalize().toFile());
	}

	/**
	 * Create a new {@link Context} instance with the specified value.
	 * @param archiveFile the source archive file
	 * @param workingDir the working directory
	 */
	Context(File archiveFile, File workingDir) {
		Assert.state(isExistingArchive(archiveFile), "Unable to find source archive");
		Assert.state(archiveFile.isDirectory() || isJarOrWar(archiveFile),
				() -> "Source archive " + archiveFile + " must end with .jar or .war");
		this.archiveFile = archiveFile;
		this.workingDir = workingDir;
		this.relativeDir = deduceRelativeDir(archiveFile.getParentFile(), this.workingDir);
	}

	private boolean isExistingArchive(@Nullable File archiveFile) {
		return archiveFile != null && (archiveFile.isFile()
				|| (archiveFile.isDirectory() && new File(archiveFile, JarFile.MANIFEST_NAME).isFile()));
	}

	private boolean isJarOrWar(File jarFile) {
		String name = jarFile.getName().toLowerCase(Locale.ROOT);
		return name.endsWith(".jar") || name.endsWith(".war");
	}

	private static File getSourceArchiveFile() {
		try {
			ProtectionDomain domain = Launcher.class.getProtectionDomain();
			CodeSource codeSource = (domain != null) ? domain.getCodeSource() : null;
			URL location = (codeSource != null) ? codeSource.getLocation() : null;
			File source = (location != null) ? findSource(location) : null;
			if (source != null && source.exists()) {
				return source.getAbsoluteFile();
			}
			throw new IllegalStateException("Unable to find source archive");
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to find source archive", ex);
		}
	}

	private static File findSource(URL location) throws IOException, URISyntaxException {
		URLConnection connection = location.openConnection();
		if (connection instanceof JarURLConnection jarURLConnection) {
			return getRootJarFile(jarURLConnection.getJarFile());
		}
		return new File(location.toURI());
	}

	private static File getRootJarFile(JarFile jarFile) {
		String name = jarFile.getName();
		int separator = name.indexOf("!/");
		if (separator > 0) {
			name = name.substring(0, separator);
		}
		return new File(name);
	}

	private @Nullable String deduceRelativeDir(File sourceDirectory, File workingDir) {
		String sourcePath = sourceDirectory.getAbsolutePath();
		String workingPath = workingDir.getAbsolutePath();
		if (sourcePath.equals(workingPath) || !sourcePath.startsWith(workingPath)) {
			return null;
		}
		String relativePath = sourcePath.substring(workingPath.length() + 1);
		return !relativePath.isEmpty() ? relativePath : null;
	}

	/**
	 * Return the source archive file that is running in tools mode.
	 * @return the archive file
	 */
	File getArchiveFile() {
		return this.archiveFile;
	}

	/**
	 * Return the current working directory.
	 * @return the working dir
	 */
	File getWorkingDir() {
		return this.workingDir;
	}

	/**
	 * Return the directory relative to {@link #getWorkingDir()} that contains the archive
	 * or {@code null} if none relative directory can be deduced.
	 * @return the relative dir ending in {@code /} or {@code null}
	 */
	@Nullable String getRelativeArchiveDir() {
		return this.relativeDir;
	}

}

/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.loader;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.ExplodedArchive;
import org.springframework.boot.loader.archive.JarFileArchive;
import org.springframework.boot.loader.jar.JarFile;

/**
 * Base class for launchers that can start an application with a fully configured
 * classpath backed by one or more {@link Archive}s.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
public abstract class Launcher {

	protected Logger logger = Logger.getLogger(Launcher.class.getName());

	/**
	 * The main runner class. This must be loaded by the created ClassLoader so cannot be
	 * directly referenced.
	 */
	private static final String RUNNER_CLASS = Launcher.class.getPackage().getName()
			+ ".MainMethodRunner";

	/**
	 * Launch the application. This method is the initial entry point that should be
	 * called by a subclass {@code public static void main(String[] args)} method.
	 * @param args the incoming arguments
	 */
	protected void launch(String[] args) {
		try {
			JarFile.registerUrlProtocolHandler();
			ClassLoader classLoader = createClassLoader(getClassPathArchives());
			launch(args, getMainClass(), classLoader);
		}
		catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Create a classloader for the specified archives.
	 * @param archives the archives
	 * @return the classloader
	 * @throws Exception
	 */
	protected ClassLoader createClassLoader(List<Archive> archives) throws Exception {
		List<URL> urls = new ArrayList<URL>(archives.size());
		for (Archive archive : archives) {
			urls.add(archive.getUrl());
		}
		return createClassLoader(urls.toArray(new URL[urls.size()]));
	}

	/**
	 * Create a classloader for the specified URLs
	 * @param urls the URLs
	 * @return the classloader
	 * @throws Exception
	 */
	protected ClassLoader createClassLoader(URL[] urls) throws Exception {
		return new LaunchedURLClassLoader(urls, getClass().getClassLoader());
	}

	/**
	 * Launch the application given the archive file and a fully configured classloader.
	 * @param args the incoming arguments
	 * @param mainClass the main class to run
	 * @param classLoader the classloader
	 * @throws Exception
	 */
	protected void launch(String[] args, String mainClass, ClassLoader classLoader)
			throws Exception {
		Runnable runner = createMainMethodRunner(mainClass, args, classLoader);
		Thread runnerThread = new Thread(runner);
		runnerThread.setContextClassLoader(classLoader);
		runnerThread.setName(Thread.currentThread().getName());
		runnerThread.start();
	}

	/**
	 * Create the {@code MainMethodRunner} used to launch the application.
	 * @param mainClass the main class
	 * @param args the incoming arguments
	 * @param classLoader the classloader
	 * @return a runnable used to start the application
	 * @throws Exception
	 */
	protected Runnable createMainMethodRunner(String mainClass, String[] args,
			ClassLoader classLoader) throws Exception {
		Class<?> runnerClass = classLoader.loadClass(RUNNER_CLASS);
		Constructor<?> constructor = runnerClass.getConstructor(String.class,
				String[].class);
		return (Runnable) constructor.newInstance(mainClass, args);
	}

	/**
	 * Returns the main class that should be launched.
	 * @return the name of the main class
	 * @throws Exception
	 */
	protected abstract String getMainClass() throws Exception;

	/**
	 * Returns the archives that will be used to construct the class path.
	 * @return the class path archives
	 * @throws Exception
	 */
	protected abstract List<Archive> getClassPathArchives() throws Exception;

	protected final Archive createArchive() throws Exception {
		ProtectionDomain protectionDomain = getClass().getProtectionDomain();
		CodeSource codeSource = protectionDomain.getCodeSource();
		URI location = (codeSource == null ? null : codeSource.getLocation().toURI());
		String path = (location == null ? null : location.getPath());
		if (path == null) {
			throw new IllegalStateException("Unable to determine code source archive");
		}
		File root = new File(path);
		if (!root.exists()) {
			throw new IllegalStateException(
					"Unable to determine code source archive from " + root);
		}
		return (root.isDirectory() ? new ExplodedArchive(root) : new JarFileArchive(root));
	}

}

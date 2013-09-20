/*
 * Copyright 2012-2013 the original author or authors.
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

import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Common convenience methods shared by launcher implementations.
 * 
 * @author Dave Syer
 */
public class LaunchHelper {

	private Logger logger = Logger.getLogger(LaunchHelper.class.getName());

	/**
	 * The main runner class. This must be loaded by the created ClassLoader so cannot be
	 * directly referenced.
	 */
	private static final String RUNNER_CLASS = AbstractLauncher.class.getPackage()
			.getName() + ".MainMethodRunner";

	/**
	 * @param args the incoming arguments
	 * @param mainClass the main class
	 * @param lib a collection of archives (zip/jar/war or directory)
	 * @throws Exception
	 */
	public void launch(String[] args, String mainClass, List<Archive> lib)
			throws Exception {
		ClassLoader classLoader = createClassLoader(lib);
		launch(args, mainClass, classLoader);
	}

	/**
	 * @param archive the archive to search
	 * @return an accumulation of nested archives
	 * @throws Exception
	 */
	public List<Archive> findNestedArchives(Archive archive, ArchiveFilter filter)
			throws Exception {
		List<Archive> lib = new ArrayList<Archive>();
		for (Archive.Entry entry : archive.getEntries()) {
			if (filter.isArchive(entry)) {
				this.logger.fine("Adding: " + entry.getName());
				lib.add(archive.getNestedArchive(entry));
			}
		}
		return lib;
	}

	/**
	 * Obtain the main class that should be used to launch the application. By default
	 * this method uses a {@code Start-Class} manifest entry.
	 * @param archive the archive
	 * @return the main class
	 * @throws Exception
	 */
	public String getMainClass(Archive archive) throws Exception {
		String mainClass = archive.getManifest().getMainAttributes()
				.getValue("Start-Class");
		if (mainClass == null) {
			throw new IllegalStateException("No 'Start-Class' manifest entry specified");
		}
		return mainClass;
	}

	/**
	 * Create a classloader for the specified lib.
	 * @param lib the lib
	 * @return the classloader
	 * @throws Exception
	 */
	protected ClassLoader createClassLoader(List<Archive> lib) throws Exception {
		URL[] urls = new URL[lib.size()];
		for (int i = 0; i < urls.length; i++) {
			urls[i] = lib.get(i).getUrl();
		}
		return createClassLoader(urls);
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
	 * Create a classloader for the specified URLs
	 * @param urls the URLs
	 * @return the classloader
	 * @throws Exception
	 */
	protected ClassLoader createClassLoader(URL[] urls) throws Exception {
		return new LaunchedURLClassLoader(urls, getClass().getClassLoader());
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

}

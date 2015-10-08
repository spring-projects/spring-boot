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

package org.springframework.boot.cli.command.run;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.springframework.boot.cli.app.SpringApplicationLauncher;
import org.springframework.boot.cli.compiler.GroovyCompiler;
import org.springframework.boot.cli.util.ResourceUtils;

/**
 * Compiles Groovy code running the resulting classes using a {@code SpringApplication}.
 * Takes care of threading and class-loading issues and can optionally monitor sources for
 * changes.
 *
 * @author Phillip Webb
 * @author Dave Syer
 */
public class SpringApplicationRunner {

	private static int watcherCounter = 0;

	private static int runnerCounter = 0;

	private final SpringApplicationRunnerConfiguration configuration;

	private final String[] sources;

	private final String[] args;

	private final GroovyCompiler compiler;

	private RunThread runThread;

	private FileWatchThread fileWatchThread;

	/**
	 * Create a new {@link SpringApplicationRunner} instance.
	 * @param configuration the configuration
	 * @param sources the files to compile/watch
	 * @param args input arguments
	 */
	SpringApplicationRunner(final SpringApplicationRunnerConfiguration configuration,
			String[] sources, String... args) {
		this.configuration = configuration;
		this.sources = sources.clone();
		this.args = args.clone();
		this.compiler = new GroovyCompiler(configuration);
		int level = configuration.getLogLevel().intValue();
		if (level <= Level.FINER.intValue()) {
			System.setProperty("groovy.grape.report.downloads", "true");
			System.setProperty("trace", "true");
		}
		else if (level <= Level.FINE.intValue()) {
			System.setProperty("debug", "true");
		}
		else if (level == Level.OFF.intValue()) {
			System.setProperty("spring.main.showBanner", "false");
			System.setProperty("logging.level.ROOT", "OFF");
		}
	}

	/**
	 * Compile and run the application. This method is synchronized as it can be called by
	 * file monitoring threads.
	 * @throws Exception on error
	 */
	public synchronized void compileAndRun() throws Exception {
		try {
			stop();
			Object[] compiledSources = compile();
			monitorForChanges();
			// Run in new thread to ensure that the context classloader is setup
			this.runThread = new RunThread(compiledSources);
			this.runThread.start();
			this.runThread.join();
		}
		catch (Exception ex) {
			if (this.fileWatchThread == null) {
				throw ex;
			}
			else {
				ex.printStackTrace();
			}
		}
	}

	public void stop() {
		if (this.runThread != null) {
			this.runThread.shutdown();
			this.runThread = null;
		}
	}

	private Object[] compile() throws IOException {
		Object[] compiledSources = this.compiler.compile(this.sources);
		if (compiledSources.length == 0) {
			throw new RuntimeException("No classes found in '" + this.sources + "'");
		}
		return compiledSources;
	}

	private void monitorForChanges() {
		if (this.fileWatchThread == null && this.configuration.isWatchForFileChanges()) {
			this.fileWatchThread = new FileWatchThread();
			this.fileWatchThread.start();
		}
	}

	/**
	 * Thread used to launch the Spring Application with the correct context classloader.
	 */
	private class RunThread extends Thread {

		private final Object[] compiledSources;

		private Object applicationContext;

		/**
		 * Create a new {@link RunThread} instance.
		 * @param compiledSources the sources to launch
		 */
		RunThread(Object... compiledSources) {
			super("runner-" + (runnerCounter++));
			this.compiledSources = compiledSources;
			if (compiledSources.length != 0 && compiledSources[0] instanceof Class) {
				setContextClassLoader(((Class<?>) compiledSources[0]).getClassLoader());
			}
			setDaemon(true);
		}

		@Override
		public void run() {
			try {
				this.applicationContext = new SpringApplicationLauncher(
						getContextClassLoader()).launch(this.compiledSources,
								SpringApplicationRunner.this.args);
			}
			catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		/**
		 * Shutdown the thread, closing any previously opened application context.
		 */
		public synchronized void shutdown() {
			if (this.applicationContext != null) {
				try {
					Method method = this.applicationContext.getClass().getMethod("close");
					method.invoke(this.applicationContext);
				}
				catch (NoSuchMethodException ex) {
					// Not an application context that we can close
				}
				catch (Exception ex) {
					ex.printStackTrace();
				}
				finally {
					this.applicationContext = null;
				}
			}
		}
	}

	/**
	 * Thread to watch for file changes and trigger recompile/reload.
	 */
	private class FileWatchThread extends Thread {

		private long previous;

		private List<File> sources;

		FileWatchThread() {
			super("filewatcher-" + (watcherCounter++));
			this.previous = 0;
			this.sources = getSourceFiles();
			for (File file : this.sources) {
				if (file.exists()) {
					long current = file.lastModified();
					if (current > this.previous) {
						this.previous = current;
					}
				}
			}
			setDaemon(false);
		}

		private List<File> getSourceFiles() {
			List<File> sources = new ArrayList<File>();
			for (String source : SpringApplicationRunner.this.sources) {
				List<String> paths = ResourceUtils.getUrls(source,
						SpringApplicationRunner.this.compiler.getLoader());
				for (String path : paths) {
					try {
						URL url = new URL(path);
						if ("file".equals(url.getProtocol())) {
							sources.add(new File(url.getFile()));
						}
					}
					catch (MalformedURLException ex) {
						// Ignore
					}
				}
			}
			return sources;
		}

		@Override
		public void run() {
			while (true) {
				try {
					Thread.sleep(TimeUnit.SECONDS.toMillis(1));
					for (File file : this.sources) {
						if (file.exists()) {
							long current = file.lastModified();
							if (this.previous < current) {
								this.previous = current;
								compileAndRun();
							}
						}
					}
				}
				catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
				catch (Exception ex) {
					// Swallow, will be reported by compileAndRun
				}
			}
		}

	}

}

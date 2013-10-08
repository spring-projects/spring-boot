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

package org.springframework.boot.cli.command;

import groovy.lang.GroovyObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import joptsimple.OptionSet;

import org.apache.ivy.util.FileUtil;
import org.springframework.boot.cli.Log;
import org.springframework.boot.cli.command.tester.Failure;
import org.springframework.boot.cli.command.tester.TestResults;
import org.springframework.boot.cli.compiler.GroovyCompiler;
import org.springframework.boot.cli.compiler.GroovyCompilerConfiguration;

/**
 * Invokes testing for autocompiled scripts
 * 
 * @author Greg Turnquist
 */
public class TestCommand extends OptionParsingCommand {

	private TestOptionHandler testOptionHandler;

	public TestCommand() {
		super("test", "Test a groovy script", new TestOptionHandler());
		this.testOptionHandler = (TestOptionHandler) this.getHandler();
	}

	@Override
	public String getUsageHelp() {
		return "[options] <files>";
	}

	public TestResults getResults() {
		return this.testOptionHandler.results;
	}

	private static class TestGroovyCompilerConfiguration implements
			GroovyCompilerConfiguration {
		@Override
		public boolean isGuessImports() {
			return true;
		}

		@Override
		public boolean isGuessDependencies() {
			return true;
		}

		@Override
		public String getClasspath() {
			return "";
		}

		public Level getLogLevel() {
			return Level.INFO;
		}
	}

	private static class TestOptionHandler extends OptionHandler {

		private final GroovyCompiler compiler;

		private TestResults results;

		public TestOptionHandler() {
			TestGroovyCompilerConfiguration configuration = new TestGroovyCompilerConfiguration();
			this.compiler = new GroovyCompiler(configuration);
			if (configuration.getLogLevel().intValue() <= Level.FINE.intValue()) {
				System.setProperty("groovy.grape.report.downloads", "true");
			}
		}

		@Override
		protected void run(OptionSet options) throws Exception {
			List<?> nonOptionArguments = options.nonOptionArguments();

			Set<File> testerFiles = new HashSet<File>();
			File[] files = getFileArguments(nonOptionArguments, testerFiles);

			/*
			 * Need to compile the code twice: The first time automatically pulls in
			 * autoconfigured libraries including test tools. Then the compiled code can
			 * be scanned to see what libraries were activated. Then it can be recompiled,
			 * with appropriate tester groovy scripts included in the same classloading
			 * context. Then the testers can be fetched and invoked through reflection
			 * against the composite AST.
			 */
			// Compile - Pass 1
			Object[] sources = this.compiler.sources(files);

			boolean testing = false;
			try {
				check("org.junit.Test", sources);
				testerFiles.add(locateSourceFromUrl("junit", "testers/junit.groovy"));
				testing = true;
			}
			catch (ClassNotFoundException e) {
			}

			try {
				check("spock.lang.Specification", sources);
				testerFiles.add(locateSourceFromUrl("spock", "testers/spock.groovy"));
				testing = true;
			}
			catch (ClassNotFoundException e) {
			}

			if (testing) {
				testerFiles.add(locateSourceFromUrl("tester", "testers/tester.groovy"));
			}

			// Compile - Pass 2 - with appropriate testers added in
			files = getFileArguments(nonOptionArguments, testerFiles);
			sources = this.compiler.sources(files);

			if (sources.length == 0) {
				throw new RuntimeException("No classes found in '" + files + "'");
			}

			List<Class<?>> testers = new ArrayList<Class<?>>();

			// Extract list of compiled classes
			List<Class<?>> compiled = new ArrayList<Class<?>>();
			for (Object source : sources) {
				if (source.getClass() == Class.class) {
					Class<?> sourceClass = (Class<?>) source;
					if (sourceClass.getSuperclass().getName().equals("AbstractTester")) {
						testers.add(sourceClass);
					}
					else {
						compiled.add((Class<?>) source);
					}
				}
			}

			this.results = new TestResults();

			for (Class<?> tester : testers) {
				GroovyObject obj = (GroovyObject) tester.newInstance();
				this.results.add((TestResults) obj.invokeMethod("findAndTest", compiled));
			}

			printReport(this.results);
		}

		private File locateSourceFromUrl(String name, String path) {
			try {
				File file = File.createTempFile(name, ".groovy");
				file.deleteOnExit();
				FileUtil.copy(getClass().getClassLoader().getResourceAsStream(path),
						file, null);
				return file;
			}
			catch (IOException ex) {
				throw new IllegalStateException("Could not create temp file for source: "
						+ name);
			}
		}

		private Class<?> check(String className, Object[] sources)
				throws ClassNotFoundException {
			Class<?> classToReturn = null;
			ClassNotFoundException classNotFoundException = null;
			for (Object source : sources) {
				try {
					classToReturn = ((Class<?>) source).getClassLoader().loadClass(
							className);
				}
				catch (ClassNotFoundException e) {
					classNotFoundException = e;
				}
			}
			if (classToReturn != null) {
				return classToReturn;
			}
			throw classNotFoundException;
		}

		private File[] getFileArguments(List<?> nonOptionArguments, Set<File> testerFiles) {
			List<File> files = new ArrayList<File>();
			for (Object option : nonOptionArguments) {
				if (option instanceof String) {
					String filename = (String) option;
					if ("--".equals(filename)) {
						break;
					}
					if (filename.endsWith(".groovy") || filename.endsWith(".java")) {
						File file = new File(filename);
						if (file.isFile() && file.canRead()) {
							files.add(file);
						}
						else {
							URL url = getClass().getClassLoader().getResource(filename);
							if (url != null) {
								if (url.toString().startsWith("file:")) {
									files.add(new File(url.toString().substring(
											"file:".length())));
								}
							}
							else {
								throw new RuntimeException("Can't find " + filename);
							}
						}
					}
				}
			}
			if (files.size() == 0) {
				throw new RuntimeException("Please specify a file to run");
			}

			for (File testerFile : testerFiles) {
				files.add(testerFile);
			}

			return files.toArray(new File[files.size()]);
		}

		private void printReport(TestResults results) throws FileNotFoundException {
			PrintWriter writer = new PrintWriter("results.txt");

			String header = "Total: " + results.getRunCount() + ", Success: "
					+ (results.getRunCount() - results.getFailureCount())
					+ ", : Failures: " + results.getFailureCount() + "\n" + "Passed? "
					+ results.wasSuccessful();

			String trailer = "";
			String trace = "";
			for (Failure failure : results.getFailures()) {
				trailer += failure.getDescription().toString();
				trace += failure.getTrace() + "\n";
			}

			writer.println(header);
			writer.println(trace);
			writer.close();

			Log.info(header);
			Log.info(trailer);
		}

	}

}

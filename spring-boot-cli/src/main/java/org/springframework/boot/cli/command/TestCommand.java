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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import joptsimple.OptionSet;

import org.codehaus.groovy.control.CompilationFailedException;
import org.springframework.boot.cli.Log;
import org.springframework.boot.cli.command.tester.Failure;
import org.springframework.boot.cli.command.tester.TestResults;
import org.springframework.boot.cli.compiler.GroovyCompiler;
import org.springframework.boot.cli.compiler.GroovyCompilerConfiguration;
import org.springframework.boot.cli.util.FileUtils;

/**
 * Invokes testing for auto-compiled scripts
 * 
 * @author Greg Turnquist
 */
public class TestCommand extends OptionParsingCommand {

	public TestCommand() {
		super("test", "Test a groovy script", new TestOptionHandler());
	}

	@Override
	public String getUsageHelp() {
		return "[options] <files>";
	}

	public TestResults getResults() {
		return ((TestOptionHandler) this.getHandler()).results;
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
	}

	private static class TestOptionHandler extends OptionHandler {

		private TestResults results;

		@Override
		protected void run(OptionSet options) throws Exception {
			TestGroovyCompilerConfiguration configuration = new TestGroovyCompilerConfiguration();
			GroovyCompiler compiler = new GroovyCompiler(configuration);

			FileOptions fileOptions = new FileOptions(options, getClass()
					.getClassLoader());

			/*
			 * Need to compile the code twice: The first time automatically pulls in
			 * autoconfigured libraries including test tools. Then the compiled code can
			 * be scanned to see what libraries were activated. Then it can be recompiled,
			 * with appropriate tester groovy scripts included in the same classloading
			 * context. Then the testers can be fetched and invoked through reflection
			 * against the composite AST.
			 */

			// Compile - Pass 1 - compile source code to see what test libraries were
			// pulled in
			Object[] sources = compiler.sources(fileOptions.getFilesArray());
			List<File> testerFiles = compileAndCollectTesterFiles(sources);

			// Compile - Pass 2 - add appropriate testers
			List<File> files = new ArrayList<File>(fileOptions.getFiles());
			files.addAll(testerFiles);
			sources = compiler.sources(files.toArray(new File[files.size()]));
			if (sources.length == 0) {
				throw new RuntimeException("No classes found in '" + files + "'");
			}

			// Extract list of compiled classes
			List<Class<?>> compiled = new ArrayList<Class<?>>();
			List<Class<?>> testers = new ArrayList<Class<?>>();
			for (Object source : sources) {
				if (source instanceof Class) {
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

		private List<File> compileAndCollectTesterFiles(Object[] sources)
				throws CompilationFailedException, IOException {
			Set<String> testerUnits = new LinkedHashSet<String>();
			List<File> testerFiles = new ArrayList<File>();
			addTesterOnClass(sources, "org.junit.Test", testerFiles, testerUnits, "junit");
			addTesterOnClass(sources, "spock.lang.Specification", testerFiles,
					testerUnits, "junit", "spock");
			if (!testerFiles.isEmpty()) {
				testerFiles.add(createTempTesterFile("tester"));
			}

			return testerFiles;
		}

		private void addTesterOnClass(Object[] sources, String className,
				List<File> testerFiles, Set<String> testerUnits, String... testerNames) {
			for (Object source : sources) {
				if (source instanceof Class<?>) {
					try {
						((Class<?>) source).getClassLoader().loadClass(className);
						for (String testerName : testerNames) {
							if (testerUnits.add(testerName)) {
								testerFiles.add(createTempTesterFile(testerName));
							}
						}
						return;
					}
					catch (ClassNotFoundException ex) {
					}
				}
			}
		}

		private File createTempTesterFile(String name) {
			try {
				File file = File.createTempFile(name, ".groovy");
				file.deleteOnExit();
				URL resource = getClass().getClassLoader().getResource(
						"testers/" + name + ".groovy");
				FileUtils.copy(resource, file);
				return file;
			}
			catch (IOException ex) {
				throw new IllegalStateException("Could not create temp file for source: "
						+ name);
			}
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
				trailer += "Failed: " + failure.getDescription().toString() + "\n";
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

/*
 * Copyright 2013-2014 the original author or authors.
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
package org.springframework.boot.cli.command.install;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.springframework.boot.cli.command.Command;
import org.springframework.boot.cli.command.OptionParsingCommand;
import org.springframework.boot.cli.command.options.CompilerOptionHandler;
import org.springframework.boot.cli.command.options.OptionSetGroovyCompilerConfiguration;
import org.springframework.boot.cli.command.status.ExitStatus;
import org.springframework.boot.cli.compiler.GroovyCompiler;
import org.springframework.boot.cli.compiler.GroovyCompilerConfiguration;
import org.springframework.boot.cli.compiler.RepositoryConfigurationFactory;
import org.springframework.boot.cli.compiler.grape.RepositoryConfiguration;
import org.springframework.boot.cli.util.Log;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.SystemPropertyUtils;

/**
 * {@link Command} to install additional dependencies into the CLI.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
public class InstallCommand extends OptionParsingCommand {

	public static Command install() {
		return new InstallCommand("install", "Install dependencies to lib directory",
				new InstallFileProcessorFactory());
	}

	public static Command uninstall() {
		return new InstallCommand("uninstall",
				"Uninstall dependencies from a lib directory",
				new UninstallFileProcessorFactory());
	}

	private InstallCommand(String name, String description, FileProcessorFactory visitor) {
		super(name, description, new InstallOptionHandler(visitor));
	}

	private static final class InstallOptionHandler extends CompilerOptionHandler {

		private FileProcessorFactory factory;
		private OptionSpec<Void> allOption;

		public InstallOptionHandler(FileProcessorFactory factory) {
			this.factory = factory;
		}

		@Override
		protected void doOptions() {
			if (this.factory.getClass().getName().contains("UninstallFile")) {
				this.allOption = option("all", "Uninstall all");
			}
		}

		@Override
		protected ExitStatus run(OptionSet options) throws Exception {

			@SuppressWarnings("unchecked")
			List<String> args = (List<String>) options.nonOptionArguments();

			if (options.has(this.allOption)) {
				if (!args.isEmpty()) {
					throw new IllegalArgumentException(
							"No non-optional arguments permitted with --all");
				}
				uninstallAllJars();
				return ExitStatus.OK;
			}
			if (args.isEmpty()) {
				throw new IllegalArgumentException(
						"Non-optional arguments required. Specify dependencies via group:artifact:version");
			}

			List<RepositoryConfiguration> repositoryConfiguration = RepositoryConfigurationFactory
					.createDefaultRepositoryConfiguration();

			GroovyCompilerConfiguration configuration = new OptionSetGroovyCompilerConfiguration(
					options, this, repositoryConfiguration) {
				@Override
				public boolean isAutoconfigure() {
					return false;
				}
			};

			GroovyCompiler groovyCompiler = new GroovyCompiler(configuration);
			try {
				if (!args.isEmpty()) {
					List<URL> initialUrls = getClassPathUrls(groovyCompiler);
					groovyCompiler.compile(createSources(args));
					List<URL> urlsToProcessor = getClassPathUrls(groovyCompiler);
					urlsToProcessor.removeAll(initialUrls);

					processJars(urlsToProcessor);
				}
			}
			catch (Exception ex) {
				String message = ex.getMessage();
				Log.error(message != null ? message : ex.getClass().toString());
			}

			return ExitStatus.OK;
		}

		private List<URL> getClassPathUrls(GroovyCompiler compiler) {
			return new ArrayList<URL>(Arrays.asList(compiler.getLoader().getURLs()));
		}

		private void processJars(List<URL> urlsToProcess) throws IOException {
			File lib = getDefaultLibDirectory();

			FileProcessor processor = this.factory.processor(lib);

			for (URL url : urlsToProcess) {
				File file = toFile(url);
				if (file.getName().endsWith(".jar")) {
					processor.processFile(file);
				}
			}
		}

		private File toFile(URL url) {
			try {
				return new File(url.toURI());
			}
			catch (URISyntaxException ex) {
				return new File(url.getPath());
			}
		}

		private void uninstallAllJars() throws IOException {
			File lib = getDefaultLibDirectory();
			File[] filesInLib = lib.listFiles();
			if (filesInLib != null) {
				FileProcessor processor = new DeleteNotTheCliProcessor();
				for (File file : filesInLib) {
					processor.processFile(file);
				}
			}
		}

		private String createSources(List<String> args) throws IOException {
			File file = File.createTempFile("SpringInstallCommand", ".groovy");
			file.deleteOnExit();
			OutputStreamWriter stream = new OutputStreamWriter(
					new FileOutputStream(file), "UTF-8");
			try {
				for (String arg : args) {
					stream.write("@Grab('" + arg + "')");
				}
				// Dummy class to force compiler to do grab
				stream.write("class Installer {}");
			}
			finally {
				stream.close();
			}
			return file.getAbsolutePath();
		}

		private static File getDefaultLibDirectory() {
			String home = SystemPropertyUtils
					.resolvePlaceholders("${spring.home:${SPRING_HOME:.}}");
			final File lib = new File(home, "lib");
			return lib;
		}

	}

	private interface FileProcessorFactory {
		FileProcessor processor(File lib);
	}

	private interface FileProcessor {
		void processFile(File file) throws IOException;
	}

	private static class DeleteNotTheCliProcessor implements FileProcessor {
		@Override
		public void processFile(File file) throws IOException {
			if (!file.getName().startsWith("spring-boot-cli")) {
				file.delete();
			}
		}

	}

	private static class InstallFileProcessorFactory implements FileProcessorFactory {

		@Override
		public FileProcessor processor(final File lib) {
			Log.info("Installing into: " + lib);
			lib.mkdirs();
			return new FileProcessor() {
				@Override
				public void processFile(File file) throws IOException {
					FileCopyUtils.copy(file, new File(lib, file.getName()));
				}
			};
		}

	}

	private static class UninstallFileProcessorFactory implements FileProcessorFactory {

		@Override
		public FileProcessor processor(final File lib) {
			Log.info("Uninstalling from: " + lib);
			return new FileProcessor() {
				@Override
				public void processFile(File file) throws IOException {
					new File(lib, file.getName()).delete();
				}
			};
		}

	}

}

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
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.codehaus.plexus.util.FileUtils;
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
import org.springframework.util.SystemPropertyUtils;

/**
 * @author Dave Syer
 *
 */
public class InstallCommand extends OptionParsingCommand {

	public static Command install() {
		return new InstallCommand("install", "Install dependencies to lib directory",
				new InstallFileVisitorFactory());
	}

	public static Command uninstall() {
		return new InstallCommand("uninstall",
				"Uninstall dependencies from a lib directory",
				new UninstallFileVisitorFactory());
	}

	private InstallCommand(String name, String description, FileVisitorFactory visitor) {
		super(name, description, new InstallOptionHandler(visitor));
	}

	private static final class InstallOptionHandler extends CompilerOptionHandler {

		private FileVisitorFactory factory;
		private OptionSpec<Void> allOption;

		public InstallOptionHandler(FileVisitorFactory factory) {
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

			Path tmpdir = Files.createTempDirectory("SpringInstallCommand")
					.toAbsolutePath();
			String grapeRoot = System.getProperty("grape.root");
			System.setProperty("grape.root", tmpdir.toString());

			GroovyCompiler groovyCompiler = new GroovyCompiler(configuration);
			try {
				if (!args.isEmpty()) {
					groovyCompiler.compile(createSources(args));
					installJars(tmpdir);
				}
				FileUtils.deleteDirectory(tmpdir.toFile());
			}
			catch (Exception e) {
				String message = e.getMessage();
				Log.error(message != null ? message : e.getClass().toString());
			}
			finally {
				if (grapeRoot != null) {
					System.setProperty("grape.root", grapeRoot);
				}
				else {
					System.clearProperty("grape.root");
				}
			}

			return ExitStatus.OK;
		}

		private void installJars(Path tmpdir) throws IOException {
			File lib = getDefaultLibDirectory();
			Files.walkFileTree(tmpdir, this.factory.visitor(lib));
		}

		private void uninstallAllJars() throws IOException {
			File lib = getDefaultLibDirectory();
			Files.walkFileTree(lib.toPath(), new DeleteNotTheCliVisitor());
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

	private interface FileVisitorFactory {
		FileVisitor<Path> visitor(File lib);
	}

	private static class DeleteNotTheCliVisitor extends SimpleFileVisitor<Path> {
		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
				throws IOException {
			String name = file.getFileName().toString();
			if (name.endsWith(".jar") && !name.startsWith("spring-boot-cli")) {
				file.toFile().delete();
			}
			return FileVisitResult.CONTINUE;
		}
	}

	private static class InstallFileVisitorFactory implements FileVisitorFactory {

		@Override
		public SimpleFileVisitor<Path> visitor(final File lib) {
			Log.info("Installing into: " + lib);
			lib.mkdirs();
			return new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
						throws IOException {
					if (file.getFileName().toString().endsWith(".jar")) {
						Files.copy(file, new FileOutputStream(new File(lib, file
								.getFileName().toString())));
						return FileVisitResult.SKIP_SIBLINGS;
					}
					return FileVisitResult.CONTINUE;
				}
			};
		}

	}

	private static class UninstallFileVisitorFactory implements FileVisitorFactory {

		@Override
		public SimpleFileVisitor<Path> visitor(final File lib) {
			Log.info("Uninstalling from: " + lib);
			if (!lib.exists()) {
				return new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
							throws IOException {
						return FileVisitResult.TERMINATE;
					}
				};
			}
			return new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
						throws IOException {
					if (file.getFileName().toString().endsWith(".jar")) {
						new File(lib, file.getFileName().toString()).delete();
						return FileVisitResult.SKIP_SIBLINGS;
					}
					return FileVisitResult.CONTINUE;
				}
			};

		}

	}

}

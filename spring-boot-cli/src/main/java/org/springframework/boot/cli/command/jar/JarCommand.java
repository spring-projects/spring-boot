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

package org.springframework.boot.cli.command.jar;

import groovy.lang.Grab;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Manifest;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.ASTTransformation;
import org.springframework.boot.cli.command.Command;
import org.springframework.boot.cli.command.OptionParsingCommand;
import org.springframework.boot.cli.command.jar.ResourceMatcher.MatchedResource;
import org.springframework.boot.cli.command.options.CompilerOptionHandler;
import org.springframework.boot.cli.command.options.OptionSetGroovyCompilerConfiguration;
import org.springframework.boot.cli.command.options.SourceOptions;
import org.springframework.boot.cli.compiler.GroovyCompiler;
import org.springframework.boot.cli.compiler.GroovyCompilerConfiguration;
import org.springframework.boot.cli.compiler.RepositoryConfigurationFactory;
import org.springframework.boot.cli.compiler.grape.RepositoryConfiguration;
import org.springframework.boot.cli.jar.PackagedSpringApplicationLauncher;
import org.springframework.boot.loader.tools.JarWriter;
import org.springframework.boot.loader.tools.Layout;
import org.springframework.boot.loader.tools.Layouts;
import org.springframework.util.Assert;

/**
 * {@link Command} to create a self-contained executable jar file from a CLI application
 * 
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
public class JarCommand extends OptionParsingCommand {

	private static final String[] DEFAULT_INCLUDES = { "public/**", "resources/**",
			"static/**", "templates/**", "META-INF/**", "*" };

	private static final String[] DEFAULT_EXCLUDES = { ".*", "repository/**", "build/**",
			"target/**", "**/*.jar", "**/*.groovy" };

	private static final Layout LAYOUT = new Layouts.Jar();

	public JarCommand() {
		super("jar", "Create a self-contained "
				+ "executable jar file from a Spring Groovy script",
				new JarOptionHandler());
	}

	@Override
	public String getUsageHelp() {
		return "[options] <jar-name> <files>";
	}

	private static final class JarOptionHandler extends CompilerOptionHandler {

		private OptionSpec<String> includeOption;

		private OptionSpec<String> excludeOption;

		@Override
		protected void doOptions() {
			this.includeOption = option(
					"include",
					"Pattern applied to directories on the classpath to find files to include in the resulting jar")
					.withRequiredArg().defaultsTo(DEFAULT_INCLUDES);
			this.excludeOption = option(
					"exclude",
					"Pattern applied to directories on the claspath to find files to exclude from the resulting jar")
					.withRequiredArg().defaultsTo(DEFAULT_EXCLUDES);
		}

		@Override
		protected ExitStatus run(OptionSet options) throws Exception {
			List<?> nonOptionArguments = new ArrayList<Object>(
					options.nonOptionArguments());
			Assert.isTrue(nonOptionArguments.size() >= 2,
					"The name of the resulting jar and at least one source file must be specified");

			File output = new File((String) nonOptionArguments.remove(0));
			Assert.isTrue(output.getName().toLowerCase().endsWith(".jar"), "The output '"
					+ output + "' is not a JAR file.");
			deleteIfExists(output);

			GroovyCompiler compiler = createCompiler(options);

			List<URL> classpath = getClassPathUrls(compiler);
			List<MatchedResource> classpathEntries = findMatchingClasspathEntries(
					classpath, options);

			String[] sources = new SourceOptions(nonOptionArguments).getSourcesArray();
			Class<?>[] compiledClasses = compiler.compile(sources);

			List<URL> dependencies = getClassPathUrls(compiler);
			dependencies.removeAll(classpath);

			writeJar(output, compiledClasses, classpathEntries, dependencies);
			return ExitStatus.OK;
		}

		private void deleteIfExists(File file) {
			if (file.exists() && !file.delete()) {
				throw new IllegalStateException("Failed to delete existing file "
						+ file.getPath());
			}
		}

		private GroovyCompiler createCompiler(OptionSet options) {
			List<RepositoryConfiguration> repositoryConfiguration = RepositoryConfigurationFactory
					.createDefaultRepositoryConfiguration();
			GroovyCompilerConfiguration configuration = new OptionSetGroovyCompilerConfiguration(
					options, this, repositoryConfiguration);
			GroovyCompiler groovyCompiler = new GroovyCompiler(configuration);
			groovyCompiler.getAstTransformations().add(0, new GrabAnnotationTransform());
			return groovyCompiler;
		}

		private List<URL> getClassPathUrls(GroovyCompiler compiler) {
			return new ArrayList<URL>(Arrays.asList(compiler.getLoader().getURLs()));
		}

		private List<MatchedResource> findMatchingClasspathEntries(List<URL> classpath,
				OptionSet options) throws IOException {
			ResourceMatcher matcher = new ResourceMatcher(
					options.valuesOf(this.includeOption),
					options.valuesOf(this.excludeOption));
			List<File> roots = new ArrayList<File>();
			for (URL classpathEntry : classpath) {
				roots.add(new File(URI.create(classpathEntry.toString())));
			}
			return matcher.find(roots);
		}

		private void writeJar(File file, Class<?>[] compiledClasses,
				List<MatchedResource> classpathEntries, List<URL> dependencies)
				throws FileNotFoundException, IOException, URISyntaxException {
			JarWriter writer = new JarWriter(file);
			try {
				addManifest(writer, compiledClasses);
				addCliClasses(writer);
				for (Class<?> compiledClass : compiledClasses) {
					addClass(writer, compiledClass);
				}
				addClasspathEntries(writer, classpathEntries);
				addDependencies(writer, dependencies);
				writer.writeLoaderClasses();
			}
			finally {
				writer.close();
			}
		}

		private void addManifest(JarWriter writer, Class<?>[] compiledClasses)
				throws IOException {
			Manifest manifest = new Manifest();
			manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
			manifest.getMainAttributes().putValue("Main-Class",
					LAYOUT.getLauncherClassName());
			manifest.getMainAttributes().putValue("Start-Class",
					PackagedSpringApplicationLauncher.class.getName());
			manifest.getMainAttributes().putValue(
					PackagedSpringApplicationLauncher.SOURCE_MANIFEST_ENTRY,
					commaDelimitedClassNames(compiledClasses));
			writer.writeManifest(manifest);
		}

		private String commaDelimitedClassNames(Class<?>[] classes) {
			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < classes.length; i++) {
				builder.append(i == 0 ? "" : ",");
				builder.append(classes[i].getName());
			}
			return builder.toString();
		}

		private void addCliClasses(JarWriter writer) throws IOException {
			addClass(writer, PackagedSpringApplicationLauncher.class);
		}

		private void addClass(JarWriter writer, Class<?> sourceClass) throws IOException {
			String name = sourceClass.getName().replace(".", "/") + ".class";
			InputStream stream = sourceClass.getResourceAsStream("/" + name);
			writer.writeEntry(name, stream);
		}

		private void addClasspathEntries(JarWriter writer, List<MatchedResource> entries)
				throws IOException {
			for (MatchedResource entry : entries) {
				if (entry.isRoot()) {
					addDependency(writer, entry.getFile());
				}
				else {
					writer.writeEntry(entry.getName(),
							new FileInputStream(entry.getFile()));
				}
			}
		}

		private void addDependencies(JarWriter writer, List<URL> urls)
				throws IOException, URISyntaxException, FileNotFoundException {
			for (URL url : urls) {
				addDependency(writer, new File(url.toURI()));
			}
		}

		private void addDependency(JarWriter writer, File dependency)
				throws FileNotFoundException, IOException {
			if (dependency.isFile()) {
				writer.writeNestedLibrary("lib/", dependency);
			}
		}

	}

	/**
	 * {@link ASTTransformation} to change {@code @Grab} annotation values.
	 */
	private static class GrabAnnotationTransform implements ASTTransformation {

		@Override
		public void visit(ASTNode[] nodes, SourceUnit source) {
			for (ASTNode node : nodes) {
				if (node instanceof ModuleNode) {
					visitModule((ModuleNode) node);
				}
			}
		}

		private void visitModule(ModuleNode module) {
			for (ClassNode classNode : module.getClasses()) {
				AnnotationNode annotation = new AnnotationNode(new ClassNode(Grab.class));
				annotation.addMember("value", new ConstantExpression("groovy"));
				classNode.addAnnotation(annotation);
			}
		}

	}

}

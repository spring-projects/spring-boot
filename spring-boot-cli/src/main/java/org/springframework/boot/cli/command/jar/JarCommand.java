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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.springframework.boot.cli.command.CompilerOptionHandler;
import org.springframework.boot.cli.command.OptionParsingCommand;
import org.springframework.boot.cli.command.SourceOptions;
import org.springframework.boot.cli.command.jar.ResourceMatcher.MatchedResource;
import org.springframework.boot.cli.compiler.GroovyCompiler;
import org.springframework.boot.cli.compiler.GroovyCompiler.CompilationCallback;
import org.springframework.boot.cli.compiler.GroovyCompilerConfiguration;
import org.springframework.boot.cli.compiler.GroovyCompilerConfigurationAdapter;
import org.springframework.boot.cli.compiler.RepositoryConfigurationFactory;
import org.springframework.boot.cli.compiler.grape.RepositoryConfiguration;
import org.springframework.boot.cli.jar.PackagedSpringApplicationLauncher;
import org.springframework.boot.loader.tools.JarWriter;
import org.springframework.boot.loader.tools.Layout;
import org.springframework.boot.loader.tools.Layouts;
import org.springframework.util.StringUtils;

/**
 * {@link Command} to create a self-contained executable jar file from a CLI application
 * 
 * @author Andy Wilkinson
 */
public class JarCommand extends OptionParsingCommand {

	private static final Layout LAYOUT = new Layouts.Jar();

	public JarCommand() {
		super(
				"jar",
				"Create a self-contained executable jar file from a Spring Groovy script",
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
					.withRequiredArg().defaultsTo("public/**", "static/**",
							"resources/**", "META-INF/**", "*");
			this.excludeOption = option(
					"exclude",
					"Pattern applied to directories on the claspath to find files to exclude from the resulting jar")
					.withRequiredArg().defaultsTo(".*", "repository/**", "build/**",
							"target/**");
		}

		@Override
		protected void run(OptionSet options) throws Exception {
			List<?> nonOptionArguments = new ArrayList<Object>(
					options.nonOptionArguments());
			if (nonOptionArguments.size() < 2) {
				throw new IllegalStateException(
						"The name of the resulting jar and at least one source file must be specified");
			}

			File output = new File((String) nonOptionArguments.remove(0));
			if (output.exists() && !output.delete()) {
				throw new IllegalStateException(
						"Failed to delete existing application jar file "
								+ output.getPath());
			}

			GroovyCompiler groovyCompiler = createCompiler(options);

			List<URL> classpathUrls = Arrays.asList(groovyCompiler.getLoader().getURLs());
			List<MatchedResource> classpathEntries = findClasspathEntries(classpathUrls,
					options);

			final Map<String, byte[]> compiledClasses = new HashMap<String, byte[]>();
			groovyCompiler.compile(new CompilationCallback() {

				@Override
				public void byteCodeGenerated(byte[] byteCode, ClassNode classNode)
						throws IOException {
					String className = classNode.getName();
					compiledClasses.put(className, byteCode);
				}

			}, new SourceOptions(nonOptionArguments).getSourcesArray());

			List<URL> dependencyUrls = new ArrayList<URL>(Arrays.asList(groovyCompiler
					.getLoader().getURLs()));
			dependencyUrls.removeAll(classpathUrls);

			JarWriter jarWriter = new JarWriter(output);

			try {
				jarWriter.writeManifest(createManifest(compiledClasses));
				addDependencies(jarWriter, dependencyUrls);
				addClasspathEntries(jarWriter, classpathEntries);
				addApplicationClasses(jarWriter, compiledClasses);
				String runnerClassName = getClassFile(PackagedSpringApplicationLauncher.class
						.getName());
				jarWriter.writeEntry(runnerClassName,
						getClass().getResourceAsStream("/" + runnerClassName));
				jarWriter.writeLoaderClasses();
			}
			finally {
				jarWriter.close();
			}
		}

		private GroovyCompiler createCompiler(OptionSet options) {
			List<RepositoryConfiguration> repositoryConfiguration = RepositoryConfigurationFactory
					.createDefaultRepositoryConfiguration();

			GroovyCompilerConfiguration configuration = new GroovyCompilerConfigurationAdapter(
					options, this, repositoryConfiguration);

			GroovyCompiler groovyCompiler = new GroovyCompiler(configuration);
			groovyCompiler.getAstTransformations().add(0, new ASTTransformation() {

				@Override
				public void visit(ASTNode[] nodes, SourceUnit source) {
					for (ASTNode node : nodes) {
						if (node instanceof ModuleNode) {
							ModuleNode module = (ModuleNode) node;
							for (ClassNode classNode : module.getClasses()) {
								AnnotationNode annotation = new AnnotationNode(
										new ClassNode(Grab.class));
								annotation.addMember("value", new ConstantExpression(
										"groovy"));
								classNode.addAnnotation(annotation);
							}
						}
					}
				}
			});
			return groovyCompiler;
		}

		private List<MatchedResource> findClasspathEntries(List<URL> classpath,
				OptionSet options) throws IOException {
			ResourceMatcher resourceCollector = new ResourceMatcher(
					options.valuesOf(this.includeOption),
					options.valuesOf(this.excludeOption));

			List<File> roots = new ArrayList<File>();

			for (URL classpathEntry : classpath) {
				roots.add(new File(URI.create(classpathEntry.toString())));
			}

			return resourceCollector.matchResources(roots);
		}

		private Manifest createManifest(final Map<String, byte[]> compiledClasses) {
			Manifest manifest = new Manifest();
			manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
			manifest.getMainAttributes()
					.putValue(
							"Application-Classes",
							StringUtils.collectionToCommaDelimitedString(compiledClasses
									.keySet()));
			manifest.getMainAttributes().putValue("Main-Class",
					LAYOUT.getLauncherClassName());
			manifest.getMainAttributes().putValue("Start-Class",
					PackagedSpringApplicationLauncher.class.getName());
			return manifest;
		}

		private void addDependencies(JarWriter jarWriter, List<URL> urls)
				throws IOException, URISyntaxException, FileNotFoundException {
			for (URL url : urls) {
				addDependency(jarWriter, new File(url.toURI()));
			}
		}

		private void addDependency(JarWriter jarWriter, File dependency)
				throws FileNotFoundException, IOException {
			if (dependency.isFile()) {
				jarWriter.writeNestedLibrary("lib/", dependency);
			}
		}

		private void addClasspathEntries(JarWriter jarWriter,
				List<MatchedResource> classpathEntries) throws IOException {
			for (MatchedResource classpathEntry : classpathEntries) {
				if (classpathEntry.isRoot()) {
					addDependency(jarWriter, classpathEntry.getFile());
				}
				else {
					jarWriter.writeEntry(classpathEntry.getPath(), new FileInputStream(
							classpathEntry.getFile()));
				}
			}
		}

		private void addApplicationClasses(JarWriter jarWriter,
				final Map<String, byte[]> compiledClasses) throws IOException {

			for (Entry<String, byte[]> entry : compiledClasses.entrySet()) {
				jarWriter.writeEntry(getClassFile(entry.getKey()),
						new ByteArrayInputStream(entry.getValue()));
			}
		}

		private String getClassFile(String className) {
			return className.replace(".", "/") + ".class";
		}

	}
}

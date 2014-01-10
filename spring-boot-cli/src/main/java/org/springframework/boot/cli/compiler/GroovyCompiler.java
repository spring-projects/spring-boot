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

package org.springframework.boot.cli.compiler;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyClassLoader.ClassCollector;
import groovy.lang.GroovyCodeSource;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.ASTTransformationVisitor;
import org.springframework.boot.cli.compiler.grape.AetherGrapeEngine;
import org.springframework.boot.cli.compiler.grape.AetherGrapeEngineFactory;
import org.springframework.boot.cli.compiler.grape.GrapeEngineInstaller;
import org.springframework.boot.cli.compiler.transformation.DependencyAutoConfigurationTransformation;
import org.springframework.boot.cli.compiler.transformation.GroovyBeansTransformation;
import org.springframework.boot.cli.compiler.transformation.ResolveDependencyCoordinatesTransformation;
import org.springframework.boot.cli.util.ResourceUtils;

/**
 * Compiler for Groovy sources. Primarily a simple Facade for
 * {@link GroovyClassLoader#parseClass(GroovyCodeSource)} with the following additional
 * features:
 * <ul>
 * <li>{@link CompilerAutoConfiguration} strategies will be read from
 * <code>META-INF/services/org.springframework.boot.cli.compiler.CompilerAutoConfiguration</code>
 * (per the standard java {@link ServiceLoader} contract) and applied during compilation</li>
 * 
 * <li>Multiple classes can be returned if the Groovy source defines more than one Class</li>
 * 
 * <li>Generated class files can also be loaded using
 * {@link ClassLoader#getResource(String)}</li>
 * 
 * <ul>
 * 
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 */
public class GroovyCompiler {

	private final ArtifactCoordinatesResolver coordinatesResolver;

	private final GroovyCompilerConfiguration configuration;

	private final ExtendedGroovyClassLoader loader;

	private final Iterable<CompilerAutoConfiguration> compilerAutoConfigurations;

	private final List<ASTTransformation> transformations;

	/**
	 * Create a new {@link GroovyCompiler} instance.
	 * @param configuration the compiler configuration
	 */
	public GroovyCompiler(final GroovyCompilerConfiguration configuration) {

		this.configuration = configuration;
		this.loader = createLoader(configuration);

		this.coordinatesResolver = new PropertiesArtifactCoordinatesResolver(this.loader);

		AetherGrapeEngine grapeEngine = AetherGrapeEngineFactory.create(this.loader,
				configuration.getRepositoryConfiguration());

		GrapeEngineInstaller.install(grapeEngine);

		this.loader.getConfiguration().addCompilationCustomizers(
				new CompilerAutoConfigureCustomizer());
		if (configuration.isAutoconfigure()) {
			this.compilerAutoConfigurations = ServiceLoader
					.load(CompilerAutoConfiguration.class);
		}
		else {
			this.compilerAutoConfigurations = Collections.emptySet();
		}

		this.transformations = new ArrayList<ASTTransformation>();
		this.transformations.add(new DependencyAutoConfigurationTransformation(
				this.loader, this.coordinatesResolver, this.compilerAutoConfigurations));
		this.transformations.add(new GroovyBeansTransformation());
		if (this.configuration.isGuessDependencies()) {
			this.transformations.add(new ResolveDependencyCoordinatesTransformation(
					this.coordinatesResolver));
		}
	}

	public ExtendedGroovyClassLoader getLoader() {
		return this.loader;
	}

	private ExtendedGroovyClassLoader createLoader(
			GroovyCompilerConfiguration configuration) {

		ExtendedGroovyClassLoader loader = new ExtendedGroovyClassLoader(
				configuration.getScope());

		for (URL url : getExistingUrls()) {
			loader.addURL(url);
		}

		for (String classpath : configuration.getClasspath()) {
			loader.addClasspath(classpath);
		}

		return loader;
	}

	private URL[] getExistingUrls() {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		if (tccl instanceof ExtendedGroovyClassLoader) {
			return ((ExtendedGroovyClassLoader) tccl).getURLs();
		}
		else {
			return new URL[0];
		}
	}

	public void addCompilationCustomizers(CompilationCustomizer... customizers) {
		this.loader.getConfiguration().addCompilationCustomizers(customizers);
	}

	public Object[] sources(String... sources) throws CompilationFailedException,
			IOException {
		List<String> compilables = new ArrayList<String>();
		List<Object> others = new ArrayList<Object>();
		for (String source : sources) {
			if (source.endsWith(".groovy") || source.endsWith(".java")) {
				compilables.add(source);
			}
			else {
				others.add(source);
			}
		}
		Class<?>[] compiled = compile(compilables.toArray(new String[compilables.size()]));
		others.addAll(0, Arrays.asList(compiled));
		return others.toArray(new Object[others.size()]);
	}

	/**
	 * Compile the specified Groovy sources, applying any
	 * {@link CompilerAutoConfiguration}s. All classes defined in the sources will be
	 * returned from this method.
	 * @param sources the sources to compile
	 * @return compiled classes
	 * @throws CompilationFailedException
	 * @throws IOException
	 */
	public Class<?>[] compile(String... sources) throws CompilationFailedException,
			IOException {

		this.loader.clearCache();
		List<Class<?>> classes = new ArrayList<Class<?>>();

		CompilerConfiguration configuration = this.loader.getConfiguration();

		CompilationUnit compilationUnit = new CompilationUnit(configuration, null,
				this.loader);
		ClassCollector collector = this.loader.createCollector(compilationUnit, null);
		compilationUnit.setClassgenCallback(collector);

		for (String source : sources) {
			List<String> paths = ResourceUtils.getUrls(source, this.loader);
			for (String path : paths) {
				compilationUnit.addSource(new URL(path));
			}
		}

		addAstTransformations(compilationUnit);

		compilationUnit.compile(Phases.CLASS_GENERATION);
		for (Object loadedClass : collector.getLoadedClasses()) {
			classes.add((Class<?>) loadedClass);
		}
		ClassNode mainClassNode = (ClassNode) compilationUnit.getAST().getClasses()
				.get(0);
		Class<?> mainClass = null;
		for (Class<?> loadedClass : classes) {
			if (mainClassNode.getName().equals(loadedClass.getName())) {
				mainClass = loadedClass;
			}
		}
		if (mainClass != null) {
			classes.remove(mainClass);
			classes.add(0, mainClass);
		}

		return classes.toArray(new Class<?>[classes.size()]);
	}

	@SuppressWarnings("rawtypes")
	private void addAstTransformations(CompilationUnit compilationUnit) {
		LinkedList[] phaseOperations = getPhaseOperations(compilationUnit);
		processConversionOperations(phaseOperations[Phases.CONVERSION]);
	}

	@SuppressWarnings("rawtypes")
	private LinkedList[] getPhaseOperations(CompilationUnit compilationUnit) {
		try {
			Field field = CompilationUnit.class.getDeclaredField("phaseOperations");
			field.setAccessible(true);
			LinkedList[] phaseOperations = (LinkedList[]) field.get(compilationUnit);
			return phaseOperations;
		}
		catch (Exception ex) {
			throw new IllegalStateException(
					"Phase operations not available from compilation unit");
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void processConversionOperations(LinkedList conversionOperations) {
		int index = getIndexOfASTTransformationVisitor(conversionOperations);
		conversionOperations.add(index, new CompilationUnit.SourceUnitOperation() {
			@Override
			public void call(SourceUnit source) throws CompilationFailedException {
				ASTNode[] nodes = new ASTNode[] { source.getAST() };
				for (ASTTransformation transformation : GroovyCompiler.this.transformations) {
					transformation.visit(nodes, source);
				}
			}
		});
	}

	private int getIndexOfASTTransformationVisitor(LinkedList<?> conversionOperations) {
		for (int index = 0; index < conversionOperations.size(); index++) {
			if (conversionOperations.get(index).getClass().getName()
					.startsWith(ASTTransformationVisitor.class.getName())) {
				return index;
			}
		}
		return conversionOperations.size();
	}

	/**
	 * {@link CompilationCustomizer} to call {@link CompilerAutoConfiguration}s.
	 */
	private class CompilerAutoConfigureCustomizer extends CompilationCustomizer {

		public CompilerAutoConfigureCustomizer() {
			super(CompilePhase.CONVERSION);
		}

		@Override
		public void call(SourceUnit source, GeneratorContext context, ClassNode classNode)
				throws CompilationFailedException {

			ImportCustomizer importCustomizer = new ImportCustomizer();

			// Additional auto configuration
			for (CompilerAutoConfiguration autoConfiguration : GroovyCompiler.this.compilerAutoConfigurations) {
				if (autoConfiguration.matches(classNode)) {
					if (GroovyCompiler.this.configuration.isGuessImports()) {
						autoConfiguration.applyImports(importCustomizer);
						importCustomizer.call(source, context, classNode);
					}
					if (source.getAST().getClasses().size() > 0
							&& classNode.equals(source.getAST().getClasses().get(0))) {
						autoConfiguration.applyToMainClass(GroovyCompiler.this.loader,
								GroovyCompiler.this.configuration, context, source,
								classNode);
					}
					autoConfiguration
							.apply(GroovyCompiler.this.loader,
									GroovyCompiler.this.configuration, context, source,
									classNode);
				}
			}
			importCustomizer.call(source, context, classNode);
		}

	}

}

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

import groovy.grape.Grape;
import groovy.lang.Grab;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyClassLoader.ClassCollector;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ImportNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
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

/**
 * Compiler for Groovy source files. Primarily a simple Facade for
 * {@link GroovyClassLoader#parseClass(File)} with the following additional features:
 * <ul>
 * <li>{@link CompilerAutoConfiguration} strategies will be read from
 * <code>META-INF/services/org.springframework.boot.cli.compiler.CompilerAutoConfiguration</code>
 * (per the standard java {@link ServiceLoader} contract) and applied during compilation</li>
 * 
 * <li>Multiple classes can be returned if the Groovy source defines more than one Class</li>
 * 
 * <li>Generated class files can also be loaded using
 * {@link ClassLoader#getResource(String)}</li>
 * <ul>
 * 
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 */
public class GroovyCompiler {

	private GroovyCompilerConfiguration configuration;

	private ExtendedGroovyClassLoader loader;

	private ArtifactCoordinatesResolver artifactCoordinatesResolver;

	private final ASTTransformation dependencyCoordinatesTransformation = new DefaultDependencyCoordinatesAstTransformation();

	/**
	 * Create a new {@link GroovyCompiler} instance.
	 * @param configuration the compiler configuration
	 */
	public GroovyCompiler(final GroovyCompilerConfiguration configuration) {
		this.configuration = configuration;
		CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
		this.loader = new ExtendedGroovyClassLoader(getClass().getClassLoader(),
				compilerConfiguration);
		if (configuration.getClasspath().length() > 0) {
			this.loader.addClasspath(configuration.getClasspath());
		}
		this.artifactCoordinatesResolver = new PropertiesArtifactCoordinatesResolver(
				this.loader);
		new GrapeEngineCustomizer(Grape.getInstance()).customize();
		compilerConfiguration
				.addCompilationCustomizers(new CompilerAutoConfigureCustomizer());

	}

	public void addCompilationCustomizers(CompilationCustomizer... customizers) {
		this.loader.getConfiguration().addCompilationCustomizers(customizers);
	}

	public Object[] sources(File... files) throws CompilationFailedException, IOException {
		List<File> compilables = new ArrayList<File>();
		List<Object> others = new ArrayList<Object>();
		for (File file : files) {
			if (file.getName().endsWith(".groovy") || file.getName().endsWith(".java")) {
				compilables.add(file);
			}
			else {
				others.add(file);
			}
		}
		Class<?>[] compiled = compile(compilables.toArray(new File[compilables.size()]));
		others.addAll(0, Arrays.asList(compiled));
		return others.toArray(new Object[others.size()]);
	}

	/**
	 * Compile the specified Groovy source files, applying any
	 * {@link CompilerAutoConfiguration}s. All classes defined in the files will be
	 * returned from this method.
	 * @param file the file to compile
	 * @return compiled classes
	 * @throws CompilationFailedException
	 * @throws IOException
	 */
	public Class<?>[] compile(File... file) throws CompilationFailedException,
			IOException {

		this.loader.clearCache();
		List<Class<?>> classes = new ArrayList<Class<?>>();

		CompilerConfiguration compilerConfiguration = this.loader.getConfiguration();

		final CompilationUnit compilationUnit = new CompilationUnit(
				compilerConfiguration, null, this.loader);
		SourceUnit sourceUnit = new SourceUnit(file[0], compilerConfiguration,
				this.loader, compilationUnit.getErrorCollector());
		ClassCollector collector = this.loader.createCollector(compilationUnit,
				sourceUnit);
		compilationUnit.setClassgenCallback(collector);

		compilationUnit.addSources(file);

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
	private void addAstTransformations(final CompilationUnit compilationUnit) {
		try {
			Field field = CompilationUnit.class.getDeclaredField("phaseOperations");
			field.setAccessible(true);
			LinkedList[] phaseOperations = (LinkedList[]) field.get(compilationUnit);
			processConversionOperations(phaseOperations[Phases.CONVERSION]);
		}
		catch (Exception npe) {
			throw new IllegalStateException(
					"Phase operations not available from compilation unit");
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void processConversionOperations(LinkedList conversionOperations) {
		for (int i = 0; i < conversionOperations.size(); i++) {
			Object operation = conversionOperations.get(i);

			if (operation.getClass().getName()
					.startsWith(ASTTransformationVisitor.class.getName())) {
				conversionOperations.add(i, new CompilationUnit.SourceUnitOperation() {
					@Override
					public void call(SourceUnit source) throws CompilationFailedException {
						GroovyCompiler.this.dependencyCoordinatesTransformation.visit(
								new ASTNode[] { source.getAST() }, source);
					}
				});
				break;
			}
		}
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

			ServiceLoader<CompilerAutoConfiguration> customizers = ServiceLoader.load(
					CompilerAutoConfiguration.class,
					GroovyCompiler.class.getClassLoader());

			// Early sweep to get dependencies
			DependencyCustomizer dependencyCustomizer = new DependencyCustomizer(
					GroovyCompiler.this.loader,
					GroovyCompiler.this.artifactCoordinatesResolver);
			for (CompilerAutoConfiguration autoConfiguration : customizers) {
				if (autoConfiguration.matches(classNode)) {
					if (GroovyCompiler.this.configuration.isGuessDependencies()) {
						autoConfiguration.applyDependencies(dependencyCustomizer);
					}
				}
			}
			dependencyCustomizer.call();

			// Additional auto configuration
			for (CompilerAutoConfiguration autoConfiguration : customizers) {
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

	private class DefaultDependencyCoordinatesAstTransformation implements
			ASTTransformation {

		@Override
		public void visit(ASTNode[] nodes, final SourceUnit source) {
			for (ASTNode node : nodes) {
				if (node instanceof ModuleNode) {
					ModuleNode module = (ModuleNode) node;
					for (ImportNode importNode : module.getImports()) {
						visitAnnotatedNode(importNode);
					}
					for (ClassNode classNode : module.getClasses()) {
						visitAnnotatedNode(classNode);
						classNode.visitContents(new ClassCodeVisitorSupport() {
							@Override
							protected SourceUnit getSourceUnit() {
								return source;
							}

							@Override
							public void visitAnnotations(AnnotatedNode node) {
								visitAnnotatedNode(node);
							}
						});
					}
				}
			}
		}

		private void visitAnnotatedNode(AnnotatedNode annotatedNode) {
			for (AnnotationNode annotationNode : annotatedNode.getAnnotations()) {
				if (isGrabAnnotation(annotationNode)) {
					transformGrabAnnotationIfNecessary(annotationNode);
				}
			}
		}

		private boolean isGrabAnnotation(AnnotationNode annotationNode) {
			String annotationClassName = annotationNode.getClassNode().getName();
			return annotationClassName.equals(Grab.class.getName())
					|| annotationClassName.equals(Grab.class.getSimpleName());
		}

		private void transformGrabAnnotationIfNecessary(AnnotationNode grabAnnotation) {
			Expression valueExpression = grabAnnotation.getMember("value");
			if (valueExpression instanceof ConstantExpression) {
				ConstantExpression constantExpression = (ConstantExpression) valueExpression;
				Object valueObject = constantExpression.getValue();
				if (valueObject instanceof String) {
					String value = (String) valueObject;
					if (!isConvenienceForm(value)) {
						transformGrabAnnotation(grabAnnotation, constantExpression);
					}
				}
			}
		}

		private boolean isConvenienceForm(String value) {
			return value.contains(":") || value.contains("#");
		}

		private void transformGrabAnnotation(AnnotationNode grabAnnotation,
				ConstantExpression moduleExpression) {

			grabAnnotation.setMember("module", moduleExpression);

			String module = (String) moduleExpression.getValue();

			if (grabAnnotation.getMember("group") == null) {
				ConstantExpression groupIdExpression = new ConstantExpression(
						GroovyCompiler.this.artifactCoordinatesResolver
								.getGroupId(module));
				grabAnnotation.setMember("group", groupIdExpression);
			}

			if (grabAnnotation.getMember("version") == null) {
				ConstantExpression versionExpression = new ConstantExpression(
						GroovyCompiler.this.artifactCoordinatesResolver
								.getVersion(module));
				grabAnnotation.setMember("version", versionExpression);
			}

		}
	}

}

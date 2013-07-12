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

package org.springframework.cli.compiler.autoconfigure;

import groovy.lang.GroovyClassLoader;

import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.springframework.cli.compiler.CompilerAutoConfiguration;
import org.springframework.cli.compiler.DependencyCustomizer;
import org.springframework.cli.compiler.GroovyCompilerConfiguration;

/**
 * {@link CompilerAutoConfiguration} for Spring.
 * 
 * @author Dave Syer
 * @author Phillip Webb
 */
public class SpringCompilerAutoConfiguration extends CompilerAutoConfiguration {

	@Override
	public void applyDependencies(DependencyCustomizer dependencies) {
		dependencies.ifAnyMissingClasses(
				"org.springframework.bootstrap.SpringApplication").add(
				"org.springframework.zero", "spring-autoconfigure",
				"0.5.0.BUILD-SNAPSHOT");
		dependencies.ifAnyResourcesPresent("logback.xml").add("ch.qos.logback",
				"logback-classic", "1.0.7");
		dependencies.ifNotAdded("cg.qos.logback", "logback-classic")
				.ifAnyResourcesPresent("log4j.properties", "log4j.xml")
				.add("org.slf4j", "slf4j-log4j12", "1.7.5")
				.add("log4j", "log4j", "1.2.16")
				.add("org.slf4j", "jcl-over-slf4j", "1.7.5");
		dependencies.ifNotAdded("ch.qos.logback", "logback-classic")
				.ifNotAdded("org.slf4j", "slf4j-log4j12")
				.add("org.slf4j", "slf4j-jdk14", "1.7.5")
				.add("org.slf4j", "jcl-over-slf4j", "1.7.5");
		// FIXME get the version
	}

	@Override
	public void applyImports(ImportCustomizer imports) {
		imports.addImports("javax.sql.DataSource", "javax.annotation.PostConstruct",
				"javax.annotation.PreDestroy", "groovy.util.logging.Log",
				"org.springframework.stereotype.Controller",
				"org.springframework.stereotype.Service",
				"org.springframework.stereotype.Component",
				"org.springframework.beans.factory.annotation.Autowired",
				"org.springframework.beans.factory.annotation.Value",
				"org.springframework.context.annotation.Import",
				"org.springframework.context.annotation.ImportResource",
				"org.springframework.context.annotation.Profile",
				"org.springframework.context.annotation.Scope",
				"org.springframework.context.annotation.Configuration",
				"org.springframework.context.annotation.ComponentScan",
				"org.springframework.context.annotation.Bean",
				"org.springframework.context.ApplicationContext",
				"org.springframework.context.MessageSource",
				"org.springframework.core.annotation.Order",
				"org.springframework.core.io.ResourceLoader",
				"org.springframework.bootstrap.CommandLineRunner",
				"org.springframework.autoconfigure.EnableAutoConfiguration");
		imports.addStarImports("org.springframework.stereotype");
	}

	@Override
	public void applyToMainClass(GroovyClassLoader loader,
			GroovyCompilerConfiguration configuration, GeneratorContext generatorContext,
			SourceUnit source, ClassNode classNode) throws CompilationFailedException {
		// FIXME: add switch for auto config
		addEnableAutoConfigurationAnnotation(source, classNode);
	}

	private void addEnableAutoConfigurationAnnotation(SourceUnit source,
			ClassNode classNode) {
		if (!hasEnableAutoConfigureAnnotation(classNode)) {
			try {
				Class<?> annotationClass = source.getClassLoader().loadClass(
						"org.springframework.autoconfigure.EnableAutoConfiguration");
				AnnotationNode annotationNode = new AnnotationNode(new ClassNode(
						annotationClass));
				classNode.addAnnotation(annotationNode);
			}
			catch (ClassNotFoundException ex) {
				throw new IllegalStateException(ex);
			}
		}
	}

	private boolean hasEnableAutoConfigureAnnotation(ClassNode classNode) {
		for (AnnotationNode node : classNode.getAnnotations()) {
			if ("EnableAutoConfiguration".equals(node.getClassNode()
					.getNameWithoutPackage())) {
				return true;
			}
		}
		return false;
	}
}

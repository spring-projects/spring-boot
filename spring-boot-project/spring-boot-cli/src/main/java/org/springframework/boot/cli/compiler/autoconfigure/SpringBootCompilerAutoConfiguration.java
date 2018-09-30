/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.cli.compiler.autoconfigure;

import groovy.lang.GroovyClassLoader;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

import org.springframework.boot.cli.compiler.CompilerAutoConfiguration;
import org.springframework.boot.cli.compiler.DependencyCustomizer;
import org.springframework.boot.cli.compiler.GroovyCompilerConfiguration;

/**
 * {@link CompilerAutoConfiguration} for Spring.
 *
 * @author Dave Syer
 * @author Phillip Webb
 */
public class SpringBootCompilerAutoConfiguration extends CompilerAutoConfiguration {

	@Override
	public void applyDependencies(DependencyCustomizer dependencies) {
		dependencies.ifAnyMissingClasses("org.springframework.boot.SpringApplication")
				.add("spring-boot-starter");
	}

	@Override
	public void applyImports(ImportCustomizer imports) {
		imports.addImports("javax.annotation.PostConstruct",
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
				"org.springframework.boot.ApplicationRunner",
				"org.springframework.boot.ApplicationArguments",
				"org.springframework.boot.CommandLineRunner",
				"org.springframework.boot.context.properties.ConfigurationProperties",
				"org.springframework.boot.context.properties.EnableConfigurationProperties",
				"org.springframework.boot.autoconfigure.EnableAutoConfiguration",
				"org.springframework.boot.autoconfigure.SpringBootApplication",
				"org.springframework.boot.context.properties.ConfigurationProperties",
				"org.springframework.boot.context.properties.EnableConfigurationProperties");
		imports.addStarImports("org.springframework.stereotype",
				"org.springframework.scheduling.annotation");
	}

	@Override
	public void applyToMainClass(GroovyClassLoader loader,
			GroovyCompilerConfiguration configuration, GeneratorContext generatorContext,
			SourceUnit source, ClassNode classNode) throws CompilationFailedException {
		addEnableAutoConfigurationAnnotation(classNode);
	}

	private void addEnableAutoConfigurationAnnotation(ClassNode classNode) {
		if (!hasEnableAutoConfigureAnnotation(classNode)) {
			AnnotationNode annotationNode = new AnnotationNode(
					ClassHelper.make("EnableAutoConfiguration"));
			classNode.addAnnotation(annotationNode);
		}
	}

	private boolean hasEnableAutoConfigureAnnotation(ClassNode classNode) {
		for (AnnotationNode node : classNode.getAnnotations()) {
			String name = node.getClassNode().getNameWithoutPackage();
			if ("EnableAutoConfiguration".equals(name)
					|| "SpringBootApplication".equals(name)) {
				return true;
			}
		}
		return false;
	}

}

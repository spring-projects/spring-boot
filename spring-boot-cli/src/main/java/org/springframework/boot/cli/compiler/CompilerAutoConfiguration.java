/*
 * Copyright 2012-2015 the original author or authors.
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
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

/**
 * Strategy that can be used to apply some auto-configuration during the
 * {@link CompilePhase#CONVERSION} Groovy compile phase.
 *
 * @author Phillip Webb
 */
public abstract class CompilerAutoConfiguration {

	/**
	 * Strategy method used to determine when compiler auto-configuration should be
	 * applied. Defaults to always.
	 * @param classNode the class node
	 * @return {@code true} if the compiler should be auto configured using this class. If
	 * this method returns {@code false} no other strategy methods will be called.
	 */
	public boolean matches(ClassNode classNode) {
		return true;
	}

	/**
	 * Apply any dependency customizations. This method will only be called if
	 * {@link #matches} returns {@code true}.
	 * @param dependencies dependency customizer
	 * @throws CompilationFailedException if the dependencies cannot be applied
	 */
	public void applyDependencies(DependencyCustomizer dependencies)
			throws CompilationFailedException {
	}

	/**
	 * Apply any import customizations. This method will only be called if
	 * {@link #matches} returns {@code true}.
	 * @param imports import customizer
	 * @throws CompilationFailedException if the imports cannot be applied
	 */
	public void applyImports(ImportCustomizer imports) throws CompilationFailedException {
	}

	/**
	 * Apply any customizations to the main class. This method will only be called if
	 * {@link #matches} returns {@code true}. This method is useful when a groovy file
	 * defines more than one class but customization only applies to the first class.
	 * @param loader the class loader being used during compilation
	 * @param configuration the compiler configuration
	 * @param generatorContext the current context
	 * @param source the source unit
	 * @param classNode the main class
	 * @throws CompilationFailedException if the customizations cannot be applied
	 */
	public void applyToMainClass(GroovyClassLoader loader,
			GroovyCompilerConfiguration configuration, GeneratorContext generatorContext,
			SourceUnit source, ClassNode classNode) throws CompilationFailedException {
	}

	/**
	 * Apply any additional configuration.
	 * @param loader the class loader being used during compilation
	 * @param configuration the compiler configuration
	 * @param generatorContext the current context
	 * @param source the source unit
	 * @param classNode the class
	 * @throws CompilationFailedException if the configuration cannot be applied
	 */
	public void apply(GroovyClassLoader loader, GroovyCompilerConfiguration configuration,
			GeneratorContext generatorContext, SourceUnit source, ClassNode classNode)
					throws CompilationFailedException {
	}

}

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

package org.springframework.boot.cli.compiler.autoconfigure;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.springframework.boot.cli.compiler.AstUtils;
import org.springframework.boot.cli.compiler.CompilerAutoConfiguration;
import org.springframework.boot.cli.compiler.DependencyCustomizer;

/**
 * {@link CompilerAutoConfiguration} for the Reactor.
 * 
 * @author Dave Syer
 */
public class ReactorCompilerAutoConfiguration extends CompilerAutoConfiguration {

	@Override
	public boolean matches(ClassNode classNode) {
		return AstUtils.hasAtLeastOneAnnotation(classNode, "EnableReactor")
				|| AstUtils.hasAtLeastOneFieldOrMethod(classNode, "Reactor");
	}

	@Override
	public void applyDependencies(DependencyCustomizer dependencies) {
		dependencies.ifAnyMissingClasses("reactor.core.Reactor")
				.add("reactor-spring-context", false).add("reactor-spring-core", false)
				.add("reactor-core");
	}

	@Override
	public void applyImports(ImportCustomizer imports) {
		imports.addImports("reactor.core.Reactor", "reactor.core.spec.Reactors",
				"reactor.core.Observable", "reactor.event.Event",
				"reactor.function.Functions", "reactor.function.Predicates",
				"reactor.function.Suppliers",
				"reactor.spring.context.annotation.Consumer",
				"reactor.spring.context.annotation.Selector",
				"reactor.spring.context.annotation.SelectorType",
				"reactor.spring.context.annotation.ReplyTo",
				"reactor.spring.context.config.EnableReactor")
				.addStarImports("reactor.event.selector.Selectors")
				.addImport("ReactorEnvironment", "reactor.core.Environment");
	}

}

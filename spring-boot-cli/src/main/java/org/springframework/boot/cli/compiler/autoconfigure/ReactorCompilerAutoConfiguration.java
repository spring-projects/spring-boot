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
				|| AstUtils.hasAtLeastOneFieldOrMethod(classNode, "EventBus");
	}

	@Override
	public void applyDependencies(DependencyCustomizer dependencies) {
		dependencies.ifAnyMissingClasses("reactor.bus.EventBus")
				.add("reactor-spring-context", false).add("reactor-spring-core", false)
				.add("reactor-bus").add("reactor-stream");
	}

	@Override
	public void applyImports(ImportCustomizer imports) {
		imports.addImports("reactor.bus.Bus", "reactor.bus.Event",
				"reactor.bus.EventBus", "reactor.fn.Function", "reactor.fn.Functions",
				"reactor.fn.Predicate", "reactor.fn.Predicates", "reactor.fn.Supplier",
				"reactor.fn.Suppliers", "reactor.spring.context.annotation.Consumer",
				"reactor.spring.context.annotation.ReplyTo",
				"reactor.spring.context.annotation.Selector",
				"reactor.spring.context.annotation.SelectorType",
				"reactor.spring.context.config.EnableReactor")
				.addStarImports("reactor.bus.selector.Selectors")
				.addImport("ReactorEnvironment", "reactor.Environment");
	}

}

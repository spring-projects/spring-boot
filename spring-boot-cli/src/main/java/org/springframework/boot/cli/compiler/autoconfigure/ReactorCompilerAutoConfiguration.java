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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.springframework.boot.cli.compiler.AstUtils;
import org.springframework.boot.cli.compiler.CompilerAutoConfiguration;
import org.springframework.boot.cli.compiler.DependencyCustomizer;

/**
 * {@link CompilerAutoConfiguration} for the Recator.
 * 
 * @author Dave Syer
 */
public class ReactorCompilerAutoConfiguration extends CompilerAutoConfiguration {

	@Override
	public boolean matches(ClassNode classNode) {
		return AstUtils.hasAtLeastOneAnnotation(classNode, "EnableReactor");
	}

	@Override
	public void applyDependencies(DependencyCustomizer dependencies) {
		dependencies
				.ifAnyMissingClasses("org.reactor.Reactor")
				.add("org.projectreactor", "reactor-spring",
						dependencies.getProperty("reactor.version"), false)
				.add("org.projectreactor", "reactor-core",
						dependencies.getProperty("reactor.version"));
	}

	@Override
	public void applyImports(ImportCustomizer imports) {
		imports.addImports("reactor.core.Reactor", "reactor.event.Event",
				"reactor.function.Consumer", "reactor.function.Functions",
				"reactor.event.selector.Selectors",
				"reactor.spring.context.annotation.On",
				"reactor.spring.context.annotation.Reply",
				EnableReactor.class.getCanonicalName()).addStarImports(
				"reactor.event.Selectors");
	}

	@Target(ElementType.TYPE)
	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface EnableReactor {

	}
}

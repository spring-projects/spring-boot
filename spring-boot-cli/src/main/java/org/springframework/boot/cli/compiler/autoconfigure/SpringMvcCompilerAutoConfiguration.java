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
import org.springframework.boot.groovy.GroovyTemplate;

/**
 * {@link CompilerAutoConfiguration} for Spring MVC.
 * 
 * @author Dave Syer
 * @author Phillip Webb
 */
public class SpringMvcCompilerAutoConfiguration extends CompilerAutoConfiguration {

	@Override
	public boolean matches(ClassNode classNode) {
		return AstUtils.hasAtLeastOneAnnotation(classNode, "Controller",
				"RestController", "EnableWebMvc");
	}

	@Override
	public void applyDependencies(DependencyCustomizer dependencies) {
		dependencies
				.ifAnyMissingClasses("org.springframework.web.servlet.mvc.Controller")
				.add("spring-boot-starter-web");
		dependencies.ifAnyMissingClasses("groovy.text.TemplateEngine").add(
				"groovy-templates");
	}

	@Override
	public void applyImports(ImportCustomizer imports) {
		imports.addStarImports("org.springframework.web.bind.annotation",
				"org.springframework.web.servlet.config.annotation",
				"org.springframework.web.servlet",
				"org.springframework.http",
				"org.springframework.web.servlet.handler", "org.springframework.http",
				"org.springframework.ui", "groovy.text");
		imports.addStaticImport(GroovyTemplate.class.getName(), "template");
	}

}

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

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.springframework.cli.compiler.AstUtils;
import org.springframework.cli.compiler.CompilerAutoConfiguration;
import org.springframework.cli.compiler.DependencyCustomizer;

/**
 * {@link CompilerAutoConfiguration} for Spring MVC.
 * 
 * @author Dave Syer
 * @author Phillip Webb
 */
public class SpringMvcCompilerAutoConfiguration extends CompilerAutoConfiguration {

	@Override
	public void applyDependencies(DependencyCustomizer dependencies) {
		dependencies
				.ifAnyMissingClasses("org.springframework.web.servlet.mvc.Controller")
				.add("org.springframework", "spring-webmvc",
						dependencies.getProperty("spring.version"));

		dependencies.ifAnyMissingClasses("org.eclipse.jetty.server.Server").add(
				"org.eclipse.jetty", "jetty-webapp",
				dependencies.getProperty("jetty.version"));

		dependencies.add("org.codehaus.groovy", "groovy-templates",
				dependencies.getProperty("groovy.version"));
		// FIXME restore Tomcat when we can get reload to work
		// dependencies.ifMissingClasses("org.apache.catalina.startup.Tomcat")
		// .add("org.apache.tomcat.embed", "tomcat-embed-core",
		// dependencies.getProperty("tomcat.version"))
		// .add("org.apache.tomcat.embed", "tomcat-embed-logging-juli",
		// dependencies.getProperty("tomcat.version"));
	}

	@Override
	public boolean matches(ClassNode classNode) {
		return AstUtils.hasAtLeastOneAnnotation(classNode, "Controller", "EnableWebMvc");
	}

	@Override
	public void applyImports(ImportCustomizer imports) {
		imports.addStarImports("org.springframework.web.bind.annotation",
				"org.springframework.web.servlet.config.annotation",
				"org.springframework.http");
		imports.addStaticImport("org.springframework.cli.template.GroovyTemplate",
				"template");
	}

}

/*
<<<<<<< HEAD
 * Copyright 2012-2014 the original author or authors.
=======
 * Copyright 2012-2013 the original author or authors.
>>>>>>> 12b17e3... Add Spring Security OAuth2 support to Spring Boot CLI
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
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.springframework.boot.cli.compiler.AstUtils;
import org.springframework.boot.cli.compiler.CompilerAutoConfiguration;
import org.springframework.boot.cli.compiler.DependencyCustomizer;

/**
 * {@link CompilerAutoConfiguration} for Spring Security OAuth2.
 *
 * @author Greg Turnquist
 */
public class SpringSecurityOAuth2CompilerAutoConfiguration extends CompilerAutoConfiguration {

	@Override
	public boolean matches(ClassNode classNode) {
		return AstUtils.hasAtLeastOneAnnotation(classNode,
				"EnableAuthorizationServer", "EnableResourceServer");
	}

	@Override
	public void applyDependencies(DependencyCustomizer dependencies) throws CompilationFailedException {
		dependencies.add("spring-security-oauth2").add("spring-boot-starter-web")
			.add("spring-boot-starter-security");
	}

	@Override
	public void applyImports(ImportCustomizer imports) throws CompilationFailedException {
		imports
			.addImports(
					"org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity")
			.addStarImports(
					"org.springframework.security.oauth2.config.annotation.web.configuration",
					"org.springframework.security.access.prepost");
	}
}

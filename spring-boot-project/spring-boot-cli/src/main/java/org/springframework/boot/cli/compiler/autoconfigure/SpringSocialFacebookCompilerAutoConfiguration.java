/*
 * Copyright 2012-2014 the original author or authors.
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
 * {@link CompilerAutoConfiguration} for Spring Social Facebook.
 *
 * @author Craig Walls
 * @since 1.1.0
 */
public class SpringSocialFacebookCompilerAutoConfiguration
		extends CompilerAutoConfiguration {

	@Override
	public boolean matches(ClassNode classNode) {
		return AstUtils.hasAtLeastOneFieldOrMethod(classNode, "Facebook");
	}

	@Override
	public void applyDependencies(DependencyCustomizer dependencies)
			throws CompilationFailedException {
		dependencies
				.ifAnyMissingClasses("org.springframework.social.facebook.api.Facebook")
				.add("spring-boot-starter-social-facebook");
	}

	@Override
	public void applyImports(ImportCustomizer imports) throws CompilationFailedException {
		imports.addStarImports("org.springframework.social.facebook.api");
		imports.addStarImports("org.springframework.social.config.annotation");
	}

}

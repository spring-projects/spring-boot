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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.springframework.cli.compiler.AstUtils;
import org.springframework.cli.compiler.CompilerAutoConfiguration;
import org.springframework.cli.compiler.DependencyCustomizer;

/**
 * {@link CompilerAutoConfiguration} for Spring Integration.
 * 
 * @author Dave Syer
 */
public class SpringIntegrationCompilerAutoConfiguration extends CompilerAutoConfiguration {

	@Override
	public boolean matches(ClassNode classNode) {
		// Slightly weird detection algorithm because there is no @Enable annotation for
		// Integration
		return AstUtils.hasAtLeastOneAnnotation(classNode, "MessageEndpoint",
				"EnableIntegrationPatterns");
	}

	@Override
	public void applyDependencies(DependencyCustomizer dependencies) {
		dependencies
				.ifAnyMissingClasses("org.springframework.integration.Message")
				.add("org.springframework.integration", "spring-integration-core",
						"2.2.3.RELEASE")
				.add("org.springframework.integration",
						"spring-integration-dsl-groovy-core", "1.0.0.M1");
		dependencies.ifAnyMissingClasses("groovy.util.XmlParser").add(
				"org.codehaus.groovy", "groovy-xml", "2.1.6");
	}

	@Override
	public void applyImports(ImportCustomizer imports) {
		imports.addImports("org.springframework.integration.Message",
				"org.springframework.integration.support.MessageBuilder",
				"org.springframework.integration.MessageChannel",
				"org.springframework.integration.MessageHeaders",
				"org.springframework.integration.annotation.MessageEndpoint",
				"org.springframework.integration.annotation.Header",
				"org.springframework.integration.annotation.Headers",
				"org.springframework.integration.annotation.Payload",
				"org.springframework.integration.annotation.Payloads",
				EnableIntegrationPatterns.class.getCanonicalName(),
				"org.springframework.integration.dsl.groovy.MessageFlow",
				"org.springframework.integration.dsl.groovy.builder.IntegrationBuilder");
	}

	@Target(ElementType.TYPE)
	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface EnableIntegrationPatterns {

	}
}

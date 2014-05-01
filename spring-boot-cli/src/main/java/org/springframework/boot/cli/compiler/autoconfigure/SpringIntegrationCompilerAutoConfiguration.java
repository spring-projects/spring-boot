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
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.springframework.boot.cli.compiler.AstUtils;
import org.springframework.boot.cli.compiler.CompilerAutoConfiguration;
import org.springframework.boot.cli.compiler.DependencyCustomizer;

/**
 * {@link CompilerAutoConfiguration} for Spring Integration.
 *
 * @author Dave Syer
 * @author Artem Bilan
 */
public class SpringIntegrationCompilerAutoConfiguration extends CompilerAutoConfiguration {

	@Override
	public boolean matches(ClassNode classNode) {
		return AstUtils.hasAtLeastOneAnnotation(classNode, "EnableIntegration")
				|| AstUtils.hasAtLeastOneAnnotation(classNode, "MessageEndpoint");
	}

	@Override
	public void applyDependencies(DependencyCustomizer dependencies) {
		dependencies.ifAnyMissingClasses(
				"org.springframework.integration.config.EnableIntegration").add(
				"spring-boot-starter-integration");
	}

	@Override
	public void applyImports(ImportCustomizer imports) {
		imports.addImports("org.springframework.messaging.Message",
				"org.springframework.messaging.MessageChannel",
				"org.springframework.messaging.PollableChannel",
				"org.springframework.messaging.SubscribableChannel",
				"org.springframework.messaging.MessageHeaders",
				"org.springframework.integration.support.MessageBuilder",
				"org.springframework.integration.channel.DirectChannel",
				"org.springframework.integration.channel.QueueChannel",
				"org.springframework.integration.channel.ExecutorChannel",
				"org.springframework.integration.core.MessagingTemplate",
				"org.springframework.integration.config.EnableIntegration");
		imports.addStarImports("org.springframework.integration.annotation");
	}
}

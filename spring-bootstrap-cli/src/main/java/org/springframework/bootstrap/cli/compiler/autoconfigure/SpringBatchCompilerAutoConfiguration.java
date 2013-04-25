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

package org.springframework.bootstrap.cli.compiler.autoconfigure;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.springframework.bootstrap.cli.compiler.AstUtils;
import org.springframework.bootstrap.cli.compiler.CompilerAutoConfiguration;
import org.springframework.bootstrap.cli.compiler.DependencyCustomizer;

/**
 * {@link CompilerAutoConfiguration} for Spring Batch.
 * 
 * @author Dave Syer
 * @author Phillip Webb
 */
public class SpringBatchCompilerAutoConfiguration extends CompilerAutoConfiguration {

	@Override
	public boolean matches(ClassNode classNode) {
		return AstUtils.hasLeastOneAnnotation(classNode, "EnableBatchProcessing");
	}

	@Override
	public void applyDependencies(DependencyCustomizer dependencies) {
		dependencies.ifAnyMissingClasses("org.springframework.batch.core.Job").add(
				"org.springframework.batch", "spring-batch-core", "2.2.0.RC1");
		dependencies.ifAnyMissingClasses("org.springframework.jdbc.core.JdbcTemplate")
				.add("org.springframework", "spring-jdbc", "4.0.0.BOOTSTRAP-SNAPSHOT");
	}

	@Override
	public void applyImports(ImportCustomizer imports) {
		imports.addImports(
				"org.springframework.batch.repeat.RepeatStatus",
				"org.springframework.batch.core.scope.context.ChunkContext",
				"org.springframework.batch.core.step.tasklet.Tasklet",
				"org.springframework.batch.core.configuration.annotation.StepScope",
				"org.springframework.batch.core.configuration.annotation.JobBuilderFactory",
				"org.springframework.batch.core.configuration.annotation.StepBuilderFactory",
				"org.springframework.batch.core.configuration.annotation.EnableBatchProcessing",
				"org.springframework.batch.core.Step",
				"org.springframework.batch.core.StepExecution",
				"org.springframework.batch.core.StepContribution",
				"org.springframework.batch.core.Job",
				"org.springframework.batch.core.JobExecution",
				"org.springframework.batch.core.JobParameter",
				"org.springframework.batch.core.JobParameters",
				"org.springframework.batch.core.launch.JobLauncher",
				"org.springframework.batch.core.converter.JobParametersConverter",
				"org.springframework.batch.core.converter.DefaultJobParametersConverter");
	}
}

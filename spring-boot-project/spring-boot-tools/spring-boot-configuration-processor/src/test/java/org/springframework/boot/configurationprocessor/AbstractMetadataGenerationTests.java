/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.configurationprocessor;

import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.test.TestConfigurationMetadataAnnotationProcessor;
import org.springframework.boot.testsupport.compiler.TestCompiler;

/**
 * Base test infrastructure for metadata generation tests.
 *
 * @author Stephane Nicoll
 */
public abstract class AbstractMetadataGenerationTests {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private TestCompiler compiler;

	@Before
	public void createCompiler() throws IOException {
		this.compiler = new TestCompiler(this.temporaryFolder);
	}

	protected TestCompiler getCompiler() {
		return this.compiler;
	}

	protected ConfigurationMetadata compile(Class<?>... types) {
		TestConfigurationMetadataAnnotationProcessor processor = new TestConfigurationMetadataAnnotationProcessor(
				this.compiler.getOutputLocation());
		this.compiler.getTask(types).call(processor);
		return processor.getMetadata();
	}

}

/*
 * Copyright 2012-present the original author or authors.
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

import java.util.function.BiConsumer;

import org.springframework.boot.configurationprocessor.model.MethodDeclaration;
import org.springframework.boot.configurationprocessor.model.TypeDeclaration;
import org.springframework.boot.configurationprocessor.model.VariableDeclaration;
import org.springframework.boot.configurationprocessor.test.ItemMetadataAssert;
import org.springframework.boot.configurationprocessor.test.RoundEnvironmentTester;
import org.springframework.boot.configurationprocessor.test.TestableAnnotationProcessor;
import org.springframework.core.test.tools.SourceFile;
import org.springframework.core.test.tools.TestCompiler;

/**
 * Base test infrastructure to test {@link PropertyDescriptor} implementations.
 *
 * @author Stephane Nicoll
 * @author Scott Frederick
 */
public abstract class PropertyDescriptorTests {

	protected String createAccessorMethodName(String prefix, String name) {
		char[] chars = name.toCharArray();
		chars[0] = Character.toUpperCase(chars[0]);
		return prefix + new String(chars, 0, chars.length);
	}

	protected MethodDeclaration getMethod(TypeDeclaration element, String name) {
		return element.getMethods().stream().filter((method) -> method.getName().equals(name)).findFirst().orElse(null);
	}

	protected VariableDeclaration getField(TypeDeclaration element, String name) {
		return element.getFields().stream().filter((field) -> field.getName().equals(name)).findFirst().orElse(null);
	}

	protected ItemMetadataAssert assertItemMetadata(MetadataGenerationEnvironment metadataEnv,
			PropertyDescriptor property) {
		return new ItemMetadataAssert(property.resolveItemMetadata("test", metadataEnv));
	}

	protected void process(Class<?> target, BiConsumer<RootElements, MetadataGenerationEnvironment> consumer) {
		BiConsumer<RoundEnvironmentTester, MetadataGenerationEnvironment> internalConsumer = (roundEnv,
				metadataEnv) -> consumer.accept((type) -> metadataEnv.getContext().getTypeDeclaration(type.getName()),
						metadataEnv);
		TestableAnnotationProcessor<MetadataGenerationEnvironment> processor = new TestableAnnotationProcessor<>(
				internalConsumer, new MetadataGenerationEnvironmentFactory());
		TestCompiler compiler = TestCompiler.forSystem()
			.withProcessors(processor)
			.withSources(SourceFile.forTestClass(target));
		compiler.compile((compiled) -> {
		});
	}

	/**
	 * Provides access to the root elements of the compilation.
	 */
	@FunctionalInterface
	protected interface RootElements {

		/**
		 * Return the root {@link TypeDeclaration} for the specified {@code type}.
		 * @param type the type of the class
		 * @return the {@link TypeDeclaration}
		 */
		TypeDeclaration getRootElement(Class<?> type);

	}

}

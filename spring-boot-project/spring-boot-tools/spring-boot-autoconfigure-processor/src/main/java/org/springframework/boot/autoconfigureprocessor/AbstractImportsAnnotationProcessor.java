/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigureprocessor;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

/**
 * An {@link AbstractProcessor} to scan for annotated classes and write the class names to
 * a file using the Spring Boot {@code .imports} format.
 *
 * @author Scott Frederick
 */
abstract class AbstractImportsAnnotationProcessor extends AbstractProcessor {

	private final List<String> qualifiedClassNames = new ArrayList<>();

	abstract String getImportsFilePath();

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		for (TypeElement annotation : annotations) {
			Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotation);
			for (Element element : elements) {
				this.qualifiedClassNames.add(element.asType().toString());
			}
		}
		if (roundEnv.processingOver()) {
			try {
				writeImportsFile();
			}
			catch (IOException ex) {
				throw new IllegalStateException("Failed to write imports file '" + getImportsFilePath() + "'", ex);
			}
		}
		return false;
	}

	private void writeImportsFile() throws IOException {
		if (!this.qualifiedClassNames.isEmpty()) {
			Filer filer = this.processingEnv.getFiler();
			FileObject file = filer.createResource(StandardLocation.CLASS_OUTPUT, "", getImportsFilePath());
			try (Writer writer = new OutputStreamWriter(file.openOutputStream(), StandardCharsets.UTF_8)) {
				Collections.sort(this.qualifiedClassNames);
				for (String className : this.qualifiedClassNames) {
					writer.append(className);
					writer.append(System.lineSeparator());
				}
			}
		}
	}

}

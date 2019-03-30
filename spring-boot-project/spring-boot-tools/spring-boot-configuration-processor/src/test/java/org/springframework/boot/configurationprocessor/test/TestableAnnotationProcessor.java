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

package org.springframework.boot.configurationprocessor.test;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

/**
 * A testable {@link Processor}.
 *
 * @param <T> the type of element to help writing assertions
 * @author Stephane Nicoll
 */
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class TestableAnnotationProcessor<T> extends AbstractProcessor {

	private final BiConsumer<RoundEnvironmentTester, T> consumer;

	private final Function<ProcessingEnvironment, T> factory;

	private T target;

	public TestableAnnotationProcessor(BiConsumer<RoundEnvironmentTester, T> consumer,
			Function<ProcessingEnvironment, T> factory) {
		this.consumer = consumer;
		this.factory = factory;
	}

	@Override
	public synchronized void init(ProcessingEnvironment env) {
		this.target = this.factory.apply(env);
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations,
			RoundEnvironment roundEnv) {
		RoundEnvironmentTester tester = new RoundEnvironmentTester(roundEnv);
		if (!roundEnv.getRootElements().isEmpty()) {
			this.consumer.accept(tester, this.target);
			return true;
		}
		return false;
	}

}

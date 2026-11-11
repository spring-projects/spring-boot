/*
 * Copyright 2012-present the original author or authors.
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
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		RoundEnvironmentTester tester = new RoundEnvironmentTester(roundEnv);
		if (!roundEnv.getRootElements().isEmpty()) {
			this.consumer.accept(tester, this.target);
			return true;
		}
		return false;
	}

}

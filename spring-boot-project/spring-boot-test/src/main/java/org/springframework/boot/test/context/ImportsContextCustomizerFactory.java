/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.test.context;

import java.lang.reflect.Method;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * {@link ContextCustomizerFactory} to allow {@code @Import} annotations to be used
 * directly on test classes.
 *
 * @author Phillip Webb
 * @see ImportsContextCustomizer
 */
class ImportsContextCustomizerFactory implements ContextCustomizerFactory {

	@Override
	public ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {
		if (AnnotatedElementUtils.findMergedAnnotation(testClass, Import.class) != null) {
			assertHasNoBeanMethods(testClass);
			return new ImportsContextCustomizer(testClass);
		}
		return null;
	}

	private void assertHasNoBeanMethods(Class<?> testClass) {
		ReflectionUtils.doWithMethods(testClass, this::assertHasNoBeanMethods);
	}

	private void assertHasNoBeanMethods(Method method) {
		Assert.state(!AnnotatedElementUtils.isAnnotated(method, Bean.class),
				"Test classes cannot include @Bean methods");
	}

}

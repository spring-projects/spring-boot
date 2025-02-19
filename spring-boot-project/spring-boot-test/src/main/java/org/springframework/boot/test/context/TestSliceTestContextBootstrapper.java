/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.test.context;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.springframework.core.ResolvableType;
import org.springframework.test.context.TestContextAnnotationUtils;
import org.springframework.test.context.TestContextBootstrapper;
import org.springframework.util.Assert;

/**
 * {@link TestContextBootstrapper} for test slices support.
 *
 * @param <T> the test slice annotation
 * @author Yanming Zhou
 * @since 3.5.0
 */
public class TestSliceTestContextBootstrapper<T extends Annotation> extends SpringBootTestContextBootstrapper {

	private final Class<T> annotationType;

	private final Method propertiesMethod;

	@SuppressWarnings("unchecked")
	public TestSliceTestContextBootstrapper() {
		this.annotationType = (Class<T>) ResolvableType.forClass(getClass())
			.as(TestSliceTestContextBootstrapper.class)
			.getGeneric(0)
			.resolve();
		Assert.notNull(this.annotationType, "'%s' doesn't contain type parameter of '%s'"
			.formatted(getClass().getName(), TestSliceTestContextBootstrapper.class.getName()));
		try {
			this.propertiesMethod = this.annotationType.getDeclaredMethod("properties");
			Assert.state(this.propertiesMethod.getReturnType() == String[].class,
					"Method '%s.properties()' should return String[]".formatted(this.annotationType.getName()));
		}
		catch (NoSuchMethodException ex) {
			throw new RuntimeException(
					"Missing method '%s.properties()', consider switching to extends from '%s' directly".formatted(
							this.annotationType.getName(), SpringBootTestContextBootstrapper.class.getName()),
					ex);
		}
	}

	@Override
	protected String[] getProperties(Class<?> testClass) {
		Annotation annotation = TestContextAnnotationUtils.findMergedAnnotation(testClass, this.annotationType);
		try {
			return (annotation != null) ? (String[]) this.propertiesMethod.invoke(annotation) : null;
		}
		catch (Exception ex) {
			throw new RuntimeException("It shouldn't happen");
		}
	}

}

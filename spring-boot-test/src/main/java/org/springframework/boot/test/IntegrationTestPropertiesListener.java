/*
 * Copyright 2013-2016 the original author or authors.
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

package org.springframework.boot.test;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Manipulate the TestContext to merge properties from {@code @IntegrationTest}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @since 1.2.0
 * @deprecated as of 1.4 as no longer used by {@code @IntegrationTest}.
 */
@Deprecated
public class IntegrationTestPropertiesListener extends AbstractTestExecutionListener {

	@Override
	public void prepareTestInstance(TestContext testContext) throws Exception {
		Class<?> testClass = testContext.getTestClass();
		AnnotationAttributes annotationAttributes = AnnotatedElementUtils
				.getMergedAnnotationAttributes(testClass,
						IntegrationTest.class.getName());
		if (annotationAttributes != null) {
			addPropertySourceProperties(testContext,
					annotationAttributes.getStringArray("value"));
		}
	}

	private void addPropertySourceProperties(TestContext testContext,
			String[] properties) {
		try {
			MergedContextConfiguration configuration = (MergedContextConfiguration) ReflectionTestUtils
					.getField(testContext, "mergedContextConfiguration");
			new MergedContextConfigurationProperties(configuration).add(properties);
		}
		catch (RuntimeException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

}

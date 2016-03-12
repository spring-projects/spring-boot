/*
 * Copyright 2012-2016 the original author or authors.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.core.env.Environment;

/**
 * Test class annotation signifying that the tests are "integration tests" and therefore
 * require full startup in the same way as a production application. Normally used in
 * conjunction with {@code @SpringApplicationConfiguration}.
 * <p>
 * If your test also uses {@code @WebAppConfiguration} consider using the
 * {@link WebIntegrationTest} instead.
 *
 * @author Dave Syer
 * @see WebIntegrationTest
 * @deprecated since 1.4.0 in favor of
 * {@link org.springframework.boot.test.context.IntegrationTest}
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@org.springframework.boot.test.context.IntegrationTest
@Deprecated
public @interface IntegrationTest {

	/**
	 * Properties in form {@literal key=value} that should be added to the Spring
	 * {@link Environment} before the test runs.
	 * @return the environment properties
	 */
	@AliasFor(annotation = org.springframework.boot.test.context.IntegrationTest.class, attribute = "value")
	String[] value() default {};

}

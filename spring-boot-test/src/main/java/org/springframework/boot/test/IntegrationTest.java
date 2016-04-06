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

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.jdbc.SqlScriptsTestExecutionListener;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

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
 * @deprecated as of 1.4 in favor of {@link SpringBootTest}
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
// Leave out the ServletTestExecutionListener because it only deals with Mock* servlet
// stuff. A real embedded application will not need the mocks.
@TestExecutionListeners(listeners = { IntegrationTestPropertiesListener.class,
		DependencyInjectionTestExecutionListener.class,
		DirtiesContextTestExecutionListener.class,
		TransactionalTestExecutionListener.class, SqlScriptsTestExecutionListener.class })
@Deprecated
public @interface IntegrationTest {

	/**
	 * Properties in form {@literal key=value} that should be added to the Spring
	 * {@link Environment} before the test runs.
	 * @return the environment properties
	 */
	String[] value() default {};

}

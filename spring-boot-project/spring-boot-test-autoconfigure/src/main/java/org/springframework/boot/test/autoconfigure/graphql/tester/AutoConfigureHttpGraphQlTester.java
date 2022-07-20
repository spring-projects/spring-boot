/*
 * Copyright 2020-2022 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.graphql.tester;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.graphql.test.tester.HttpGraphQlTester;

/**
 * Annotation that can be applied to a test class to enable a {@link HttpGraphQlTester}.
 *
 * <p>
 * This annotation should be used with
 * {@link org.springframework.boot.test.context.SpringBootTest @SpringBootTest} tests with
 * Spring MVC or Spring WebFlux mock infrastructures.
 *
 * @author Brian Clozel
 * @since 2.7.0
 * @see HttpGraphQlTesterAutoConfiguration
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@AutoConfigureMockMvc
@AutoConfigureWebTestClient
@ImportAutoConfiguration
public @interface AutoConfigureHttpGraphQlTester {

}

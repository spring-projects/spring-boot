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

package org.springframework.boot.test.autoconfigure.restdocs;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.restassured.RestAssured;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.properties.PropertyMapping;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Annotation that can be applied to a test class to enable and configure
 * auto-configuration of Spring REST Docs. The auto-configuration sets up
 * {@link MockMvc}-based testing of a servlet web application, {@link WebTestClient}-based
 * testing of a reactive web application, or {@link RestAssured}-based testing of any web
 * application over HTTP.
 * <p>
 * Allows configuration of the output directory and the host, scheme, and port of
 * generated URIs. When further configuration is required a
 * {@link RestDocsMockMvcConfigurationCustomizer},
 * {@link RestDocsWebTestClientConfigurationCustomizer}, or
 * {@link RestDocsRestAssuredConfigurationCustomizer} bean can be used.
 *
 * @author Andy Wilkinson
 * @since 1.4.0
 * @see RestDocsAutoConfiguration
 * @see RestDocsMockMvcConfigurationCustomizer
 * @see RestDocsWebTestClientConfigurationCustomizer
 * @see RestDocsRestAssuredConfigurationCustomizer
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ImportAutoConfiguration
@Import(RestDocumentationContextProviderRegistrar.class)
@PropertyMapping("spring.test.restdocs")
public @interface AutoConfigureRestDocs {

	/**
	 * The output directory to which generated snippets will be written. An alias for
	 * {@link #outputDir}.
	 * @return the output directory
	 */
	@AliasFor("outputDir")
	String value() default "";

	/**
	 * The output directory to which generated snippets will be written. An alias for
	 * {@link #value}.
	 * @return the output directory
	 */
	@AliasFor("value")
	String outputDir() default "";

	/**
	 * The scheme (typically {@code http} or {@code https}) to be used in documented URIs.
	 * Defaults to {@code http}.
	 * @return the scheme
	 */
	String uriScheme() default "http";

	/**
	 * The host to be used in documented URIs. Defaults to {@code localhost}.
	 * @return the host
	 */
	String uriHost() default "localhost";

	/**
	 * The port to be used in documented URIs. Defaults to {@code 8080}.
	 * @return the port
	 */
	int uriPort() default 8080;

}

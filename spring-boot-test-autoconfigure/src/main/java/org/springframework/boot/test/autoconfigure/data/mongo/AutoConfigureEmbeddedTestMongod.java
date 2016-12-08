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

package org.springframework.boot.test.autoconfigure.data.mongo;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.properties.PropertyMapping;

/**
 * Annotation that can be applied to a test class to disable the usage of an
 * embedded Mongod even if
 * {@code de.flapdoodle.embed:de.flapdoodle.embed.mongo} is on the classpath.
 *
 * @author Michael J. Simons
 * @since 1.5.0
 * @see EmbeddedTestMongodAutoConfiguration
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ImportAutoConfiguration
@PropertyMapping("spring.test.mongod.embedded")
public @interface AutoConfigureEmbeddedTestMongod {

	/**
	 * Determines if the embedded test Mongod should be enabled if the library is available on the test classpath.
	 * @return flag wether to enable embedded test Mongod or not
	 */
	boolean enabled() default true;
}

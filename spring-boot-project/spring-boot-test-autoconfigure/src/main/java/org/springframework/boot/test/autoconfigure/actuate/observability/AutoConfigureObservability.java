/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.actuate.observability;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;

/**
 * Annotation that can be applied to a test class to enable auto-configuration for
 * observability.
 * <p>
 * If this annotation is applied to a sliced test, an in-memory {@code MeterRegistry}, a
 * no-op {@code Tracer} and an {@code ObservationRegistry} are added to the application
 * context.
 *
 * @author Moritz Halbritter
 * @since 3.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ImportAutoConfiguration
public @interface AutoConfigureObservability {

	/**
	 * Whether metrics should be reported to external systems in the test.
	 * @return whether metrics should be reported to external systems in the test
	 */
	boolean metrics() default true;

	/**
	 * Whether traces should be reported to external systems in the test.
	 * @return whether traces should be reported to external systems in the test
	 */
	boolean tracing() default true;

}

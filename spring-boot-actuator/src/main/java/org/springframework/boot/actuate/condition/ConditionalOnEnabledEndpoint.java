/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.condition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * {@link Conditional} that checks whether or not an endpoint is enabled. Matches if the
 * value of the {@code endpoints.<name>.enabled} property is {@code true}. Does not match
 * if the property's value or {@code enabledByDefault} is {@code false}. Otherwise,
 * matches if the value of the {@code endpoints.enabled} property is {@code true} or if
 * the property is not configured.
 *
 * @author Andy Wilkinson
 * @since 1.2.4
 */
@Conditional(OnEnabledEndpointCondition.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ConditionalOnEnabledEndpoint {

	/**
	 * The name of the endpoint.
	 * @return The name of the endpoint
	 */
	String value();

	/**
	 * Returns whether or not the endpoint is enabled by default.
	 * @return {@code true} if the endpoint is enabled by default, otherwise {@code false}
	 */
	boolean enabledByDefault() default true;

}

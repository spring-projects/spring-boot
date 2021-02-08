/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.actuate.endpoint.EndpointFilter;

/**
 * Annotation that can be used on an {@link Endpoint @Endpoint} to implement implicit
 * filtering. Often used as a meta-annotation on technology specific endpoint annotations,
 * for example:<pre class="code">
 * &#64;Endpoint
 * &#64;FilteredEndpoint(WebEndpointFilter.class)
 * public &#64;interface WebEndpoint {
 *
 *     &#64;AliasFor(annotation = Endpoint.class, attribute = "id")
 *     String id();
 *
 *     &#64;AliasFor(annotation = Endpoint.class, attribute = "enableByDefault")
 *     boolean enableByDefault() default true;
 *
 * } </pre>
 * @author Phillip Webb
 * @since 2.0.0
 * @see DiscovererEndpointFilter
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FilteredEndpoint {

	/**
	 * The filter class to use.
	 * @return the filter class
	 */
	Class<? extends EndpointFilter<?>> value();

}

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

package org.springframework.boot.actuate.endpoint.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.aot.hint.annotation.Reflective;
import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.Operation;
import org.springframework.core.annotation.AliasFor;

/**
 * Annotation primarily used as a meta-annotation to indicate that an annotation provides
 * extension support for an endpoint. Extensions allow additional technology specific
 * {@link Operation operations} to be added to an existing endpoint. For example, a web
 * extension may offer variations of a read operation to support filtering based on a
 * query parameter.
 * <p>
 * Extension annotations must provide an {@link EndpointFilter} to restrict when the
 * extension applies. The {@code endpoint} attribute is usually re-declared using
 * {@link AliasFor @AliasFor}. For example: <pre class="code">
 * &#64;EndpointExtension(filter = WebEndpointFilter.class)
 * public &#64;interface EndpointWebExtension {
 *
 *   &#64;AliasFor(annotation = EndpointExtension.class, attribute = "endpoint")
 *   Class&lt;?&gt; endpoint();
 *
 * }
 * </pre>
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Reflective
public @interface EndpointExtension {

	/**
	 * The filter class used to determine when the extension applies.
	 * @return the filter class
	 */
	Class<? extends EndpointFilter<?>> filter();

	/**
	 * The class of the endpoint to extend.
	 * @return the class endpoint to extend
	 */
	Class<?> endpoint() default Void.class;

}

/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.actuate.endpoint.EndpointId;

/**
 * Identifies a type as being an actuator endpoint that provides information about the
 * running application. Endpoints can be exposed over a variety of technologies including
 * JMX and HTTP.
 * <p>
 * Most {@code @Endpoint} classes will declare one or more
 * {@link ReadOperation @ReadOperation}, {@link WriteOperation @WriteOperation},
 * {@link DeleteOperation @DeleteOperation} annotated methods which will be automatically
 * adapted to the exposing technology (JMX, Spring MVC, Spring WebFlux, Jersey etc.).
 * <p>
 * {@code @Endpoint} represents the lowest common denominator for endpoints and
 * intentionally limits the sorts of operation methods that may be defined in order to
 * support the broadest possible range of exposure technologies. If you need deeper
 * support for a specific technology you can either write an endpoint that is
 * {@link FilteredEndpoint filtered} to a certain technology, or provide
 * {@link EndpointExtension extension} for the broader endpoint.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 2.0.0
 * @see EndpointExtension
 * @see FilteredEndpoint
 * @see EndpointDiscoverer
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Endpoint {

	/**
	 * The id of the endpoint (must follow {@link EndpointId} rules).
	 * @return the id
	 * @see EndpointId
	 */
	String id() default "";

	/**
	 * If the endpoint should be enabled or disabled by default.
	 * @return {@code true} if the endpoint is enabled by default
	 */
	boolean enableByDefault() default true;

}

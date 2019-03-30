/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.web.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.FilteredEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.core.annotation.AliasFor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Identifies a type as being a REST endpoint that is only exposed over Spring MVC or
 * Spring WebFlux. Mapped methods must be annotated with {@link GetMapping @GetMapping},
 * {@link PostMapping @PostMapping}, {@link DeleteMapping @DeleteMapping}, etc annotations
 * rather than {@link ReadOperation @ReadOperation},
 * {@link WriteOperation @WriteOperation}, {@link DeleteOperation @DeleteOperation}.
 * <p>
 * This annotation can be used when deeper Spring integration is required, but at the
 * expense of portability. Most users should prefer the {@link Endpoint @Endpoint} or
 * {@link WebEndpoint @WebEndpoint} annotations whenever possible.
 *
 * @author Phillip Webb
 * @since 2.0.0
 * @see WebEndpoint
 * @see ControllerEndpoint
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Endpoint
@FilteredEndpoint(ControllerEndpointFilter.class)
@ResponseBody
public @interface RestControllerEndpoint {

	/**
	 * The id of the endpoint.
	 * @return the id
	 */
	@AliasFor(annotation = Endpoint.class)
	String id();

	/**
	 * If the endpoint should be enabled or disabled by default.
	 * @return {@code true} if the endpoint is enabled by default
	 */
	@AliasFor(annotation = Endpoint.class)
	boolean enableByDefault() default true;

}

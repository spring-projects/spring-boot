/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.mvc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Specialized {@link RequestMapping} for {@link RequestMethod#GET GET} requests that
 * produce {@code application/json} or
 * {@code application/vnd.spring-boot.actuator.v1+json} responses.
 *
 * @author Andy Wilkinson
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RequestMapping(method = RequestMethod.GET, produces = {
		ActuatorMediaTypes.APPLICATION_ACTUATOR_V1_JSON_VALUE,
		MediaType.APPLICATION_JSON_VALUE })
@interface ActuatorGetMapping {

	/**
	 * Alias for {@link RequestMapping#value}.
	 * @return the value
	 */
	@AliasFor(annotation = RequestMapping.class)
	String[] value() default {};

}

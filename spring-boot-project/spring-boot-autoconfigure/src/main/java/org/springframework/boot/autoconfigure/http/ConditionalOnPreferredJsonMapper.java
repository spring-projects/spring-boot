/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.autoconfigure.http;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * {@link Conditional @Conditional} that matches based on the preferred JSON mapper. A
 * preference is expressed using the {@code spring.http.converters.preferred-json-mapper}
 * configuration property, falling back to the
 * {@code spring.mvc.converters.preferred-json-mapper} configuration property. When no
 * preference is expressed Jackson is preferred by default.
 *
 * @author Andy Wilkinson
 */
@Conditional(OnPreferredJsonMapperCondition.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@interface ConditionalOnPreferredJsonMapper {

	JsonMapper value();

	enum JsonMapper {

		GSON,

		JACKSON,

		JSONB,

	}

}

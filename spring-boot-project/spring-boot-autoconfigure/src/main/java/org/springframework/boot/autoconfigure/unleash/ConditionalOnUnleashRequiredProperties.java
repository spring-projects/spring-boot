/*
 * Copyright 2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.unleash;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Shortcut for <pre class="code">
 * &#064;@ConditionalOnProperty({
 *     "spring.unleash.app-name",
 *     "spring.unleash.unleash-api",
 *     "spring.unleash.api-key"
 * })
 * </pre>
 *
 * @author Max Schwaab
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@ConditionalOnProperty({ "spring.unleash.app-name", "spring.unleash.unleash-api", "spring.unleash.api-key" })
@interface ConditionalOnUnleashRequiredProperties {

}

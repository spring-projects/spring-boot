/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.context.properties;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that can be used to indicate that configuration properties should be bound
 * using constructor arguments rather than by calling setters. Can be added at the type
 * level (if there is an unambiguous constructor) or on the actual constructor to use.
 * <p>
 * Note: To use constructor binding the class must be enabled using
 * {@link EnableConfigurationProperties @EnableConfigurationProperties} or configuration
 * property scanning. Constructor binding cannot be used with beans that are created by
 * the regular Spring mechanisms (e.g.
 * {@link org.springframework.stereotype.Component @Component} beans, beans created via
 * {@link org.springframework.context.annotation.Bean @Bean} methods or beans loaded using
 * {@link org.springframework.context.annotation.Import @Import}).
 *
 * @author Phillip Webb
 * @since 2.2.0
 * @see ConfigurationProperties
 */
@Target({ ElementType.TYPE, ElementType.CONSTRUCTOR })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConstructorBinding {

}

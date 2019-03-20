/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.test.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

/**
 * {@link Component @Component} that can be used when a bean is intended only for tests,
 * and should be excluded from Spring Boot's component scanning.
 * <p>
 * Note that if you directly use {@link ComponentScan @ComponentScan} rather than relying
 * on {@code @SpringBootApplication} you should ensure that a {@link TypeExcludeFilter} is
 * declared as an {@link ComponentScan#excludeFilters() excludeFilter}.
 *
 * @author Phillip Webb
 * @since 1.4.0
 * @see TypeExcludeFilter
 * @see TestConfiguration
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface TestComponent {

	/**
	 * The value may indicate a suggestion for a logical component name, to be turned into
	 * a Spring bean in case of an auto-detected component.
	 * @return the specified bean name, if any
	 */
	String value() default "";

}

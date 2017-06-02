/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.condition;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.env.Environment;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * {@link Conditional} that checks if a {@link ConfigurationProperties} class' 'enabled'
 * property have the value 'true'(i.e. the value of <code>{@link ConfigurationProperties#prefix()} + "." + "enabled"</code> should be 'true').
 * the properties must be present in the {@link Environment}
 *
 * <pre><code>
 * // Example:
 * &#064;{@link ConfigurationProperties}("spring.foo")
 * &#064;{@link ConditionalOnEnabledProperty}
 * public class Foo {
 * }
 * // The old way of achieving this:
 * &#064;{@link ConfigurationProperties}("spring.foo")
 * &#064;{@link ConditionalOnProperty}({@link ConditionalOnProperty#prefix() prefix}="spring.foo", {@link ConditionalOnProperty#name() name}="enabled", {@link ConditionalOnProperty#havingValue() havingValue}="true")
 * public class Foo {
 * }
 * </code></pre>
 *
 * @author 20 Apr 2017 idosu(Ido Sorozon)
 * @see OnEnabledPropertyCondition
 * @see ConfigurationProperties
 * @see ConditionalOnProperty
 */
@Retention(RUNTIME)
@Target({ TYPE, METHOD })
@Documented
@Conditional(OnEnabledPropertyCondition.class)
public @interface ConditionalOnEnabledProperty {
}

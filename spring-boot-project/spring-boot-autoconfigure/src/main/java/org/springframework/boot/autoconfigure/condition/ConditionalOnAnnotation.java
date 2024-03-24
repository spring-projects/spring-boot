/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.autoconfigure.condition;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * {@link Conditional @Conditional} that only matches when the specified annotation is
 * present on the classpath. This conditional annotation allows for configuration beans to
 * be conditionally registered based on the presence of custom annotations.
 *
 * <p>
 * The conditional annotation is particularly useful when you want to enable a certain
 * configuration only if a specific custom annotation is declared anywhere within the
 * application codebase. For example, you could activate certain auto-configuration beans
 * if a custom {@code @EnableFeatureX} annotation is present on any of the configuration
 * classes.
 *
 * <p>
 * The annotated class will only be parsed and the bean will be registered if the
 * annotation represented by {@code value()} is present and has a {@link RetentionPolicy}
 * of {@link RetentionPolicy#RUNTIME}. If the annotation is not present or does not have a
 * RUNTIME retention policy, the bean will not be registered.
 *
 * <p>
 * Usage example: <pre class="code">
 * &#64;Configuration
 * &#64;ConditionalOnAnnotation(EnableFeatureX.class)
 * public class FeatureXConfiguration {
 *     // Bean definitions
 * }
 * </pre>
 *
 * <p>
 * In the example above, {@code FeatureXConfiguration} will only be registered if
 * {@code EnableFeatureX} annotation is available at runtime on any class in the
 * application.
 *
 * @author Feng, Liu
 * @since 3.4.0
 * @see Conditional
 * @see ConditionalOnExpression
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@Conditional(OnAnnotationCondition.class)
public @interface ConditionalOnAnnotation {

	/**
	 * The annotation that must be present in order for the condition to match. The
	 * condition does not match if this annotation is not present or does not have a
	 * RUNTIME retention policy.
	 * @return the annotation class that must be present
	 */
	Class<? extends Annotation> value();

}

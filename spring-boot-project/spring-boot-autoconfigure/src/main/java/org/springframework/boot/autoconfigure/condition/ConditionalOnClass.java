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

package org.springframework.boot.autoconfigure.condition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * {@link Conditional @Conditional} that only matches when the specified classes are on
 * the classpath.
 * <p>
 * A {@code Class} {@link #value() value} can be safely specified on
 * {@code @Configuration} classes as the annotation metadata is parsed by using ASM before
 * the class is loaded. If a class reference cannot be used then a {@link #name() name}
 * {@code String} attribute can be used.
 * <p>
 * <b>Note:</b> Extra care must be taken when using {@code @ConditionalOnClass} on
 * {@code @Bean} methods where typically the return type is the target of the condition.
 * Before the condition on the method applies, the JVM will have loaded the class and
 * potentially processed method references which will fail if the class is not present. To
 * handle this scenario, a separate {@code @Configuration} class should be used to isolate
 * the condition. For example: <pre class="code">
 * &#064;AutoConfiguration
 * public class MyAutoConfiguration {
 *
 * 	&#64;Configuration(proxyBeanMethods = false)
 * 	&#64;ConditionalOnClass(SomeService.class)
 * 	public static class SomeServiceConfiguration {
 *
 * 		&#64;Bean
 * 		&#64;ConditionalOnMissingBean
 * 		public SomeService someService() {
 * 			return new SomeService();
 * 		}
 *
 * 	}
 *
 * }</pre>
 *
 * @author Phillip Webb
 * @since 1.0.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnClassCondition.class)
public @interface ConditionalOnClass {

	/**
	 * The classes that must be present. Since this annotation is parsed by loading class
	 * bytecode, it is safe to specify classes here that may ultimately not be on the
	 * classpath, only if this annotation is directly on the affected component and
	 * <b>not</b> if this annotation is used as a composed, meta-annotation. In order to
	 * use this annotation as a meta-annotation, only use the {@link #name} attribute.
	 * @return the classes that must be present
	 */
	Class<?>[] value() default {};

	/**
	 * The classes names that must be present.
	 * @return the class names that must be present.
	 */
	String[] name() default {};

}

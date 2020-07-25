package org.springframework.boot.autoconfigure.condition;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.*;

/**
 * Indicates the Annotation class(es) which need to be checked on the main boot class
 * annotated with @SpringBootApplication. @ConditionalOnAnnotation annotation should work
 * on any @Configuration class, but the order of loading is not guaranteed. So annotating
 * at @SpringBootApplication main class makes sure this bean is loaded by the time the
 * condition is evaluated.
 *
 * Usage Examples: 1. @ConditionalOnAnnotation(EnableAsync.class)
 * 2. @ConditionalOnAnnotation({ EnableAsync.class, EnableCaching.class })
 * 3. @ConditionalOnAnnotation( value = { EnableAsync.class, EnableCaching.class },
 * conditionType = ConditionalOnAnnotation.ConditionType.AND )
 *
 * This Annotation will be useful when making a AutoConfiguration class as conditional.
 *
 * <p>
 * When placed on a {@code @Configuration} class, the configuration takes place only when
 * the specified annotation is present on main spring boot class.
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;ConditionalOnAnnotation(EnableAsync.class)
 * public class MyAutoConfiguration {
 *
 *     &#064;Bean
 *     public MyService myService() {
 *         ...
 *     }
 * }</pre>
 * <p>
 *
 *
 * <p>
 * When placed on a {@code @Bean} method, the bean is created only when the specified
 * annotation is present on main spring boot class.
 *
 * <pre class="code">
 * &#064;Configuration
 * public class MyAppConfiguration {
 *
 *     &#064;ConditionalOnAnnotation(EnableAsync.class)
 *     &#064;Bean
 *     public MyService myService() {
 *         ...
 *     }
 * }</pre>
 * <p>
 *
 * @author Vimalraj Chandra Sekaran (Github: rajvimalc)
 * @since 2.3.2
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Conditional(OnAnnotationCondition.class)
public @interface ConditionalOnAnnotation {

	/**
	 * Minimum 1 Annotation type class should be provided.
	 */
	Class<? extends Annotation>[] value();

	/**
	 * When ConditionType is OR, the condition is true when any one of the annotations
	 * mentioned is present. When ConditionType is AND, the condition is true when all of
	 * the annotations mentioned are present.
	 */
	ConditionType conditionType() default ConditionType.OR;

	enum ConditionType {

		OR, AND

	}

}

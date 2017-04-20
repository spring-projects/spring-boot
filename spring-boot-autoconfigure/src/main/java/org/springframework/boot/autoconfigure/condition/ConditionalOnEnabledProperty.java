package org.springframework.boot.autoconfigure.condition;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * TODO: desc
 *
 * @author
 * 		<br>20 Apr 2017 idosu
 */
@Retention(RUNTIME)
@Target({ TYPE, METHOD })
@Documented
@Conditional(OnEnabledPropertyCondition.class)
public @interface ConditionalOnEnabledProperty {
}

package org.springframework.boot.autoconfigure.condition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.naming.InitialContext;

import org.springframework.context.annotation.Conditional;
/**
 * {@link Conditional} that matches based on the availability of a cores
 * {@link InitialContext} and the ability to lookup specific locations.
 *
 * @author Javier Ledo Vázquez (jledo@paradigmadigital.com) 
 * @since 1.3.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnCore.class)
public @interface ConditionalOnCores {
	/**
	 * The number of cores in your system
	 * @return Number of cores
	 */
	int value();
	/**
	 * Operators 
	 * @return Operators
	 */
	Operators operator() default Operators.EQ;
}

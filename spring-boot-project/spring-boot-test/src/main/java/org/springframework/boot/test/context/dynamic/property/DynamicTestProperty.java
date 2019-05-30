package org.springframework.boot.test.context.dynamic.property;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation provides an ability to set values
 * of properties in the Spring Context before test execution.
 * You can use this annotation on the method which provides
 * a dynamic value of properties and this value will be
 * evaluated while start application context for tests.
 *
 * @author Anatoliy Korovin
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DynamicTestProperty {

}

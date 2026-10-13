/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.autoconfigure.json;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.PropertyMapping;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.boot.test.json.GsonTester;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonbTester;

/**
 * Annotation that can be applied to a test class to enable and configure
 * auto-configuration of JSON testers.
 *
 * @author Phillip Webb
 * @since 1.4.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@AutoConfigureJson
@ImportAutoConfiguration
@PropertyMapping("spring.test.jsontesters")
public @interface AutoConfigureJsonTesters {

	/**
	 * If {@link BasicJsonTester}, {@link JacksonTester}, {@link JsonbTester} and
	 * {@link GsonTester} beans should be registered. Defaults to {@code true}.
	 * @return if tester support is enabled
	 */
	boolean enabled() default true;

}

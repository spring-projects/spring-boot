/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.testsupport.classpath.resources.WithResource;

/**
 * Makes an {@code org/springframework/boot/sample-beans.xml} resource available from the
 * thread context classloader.
 *
 * @author Andy Wilkinson
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@WithResource(name = "org/springframework/boot/sample-beans.xml",
		content = """
				<?xml version="1.0" encoding="UTF-8"?>
				<beans xmlns="http://www.springframework.org/schema/beans"
					xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
					xmlns:context="http://www.springframework.org/schema/context"
					xsi:schemaLocation="http://www.springframework.org/schema/beans https://www.springframework.org/schema/beans/spring-beans.xsd
								http://www.springframework.org/schema/context https://www.springframework.org/schema/context/spring-context.xsd">
					<bean id="myXmlComponent" class="org.springframework.boot.sampleconfig.MyComponent"/>
				</beans>
				""")
@interface WithSampleBeansXmlResource {

}

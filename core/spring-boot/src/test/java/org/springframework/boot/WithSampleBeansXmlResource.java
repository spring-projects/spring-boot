/*
 * Copyright 2012-present the original author or authors.
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

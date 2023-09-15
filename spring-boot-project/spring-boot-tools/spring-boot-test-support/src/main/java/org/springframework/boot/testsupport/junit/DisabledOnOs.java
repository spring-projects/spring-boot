/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.testsupport.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Improves JUnit5's {@link org.junit.jupiter.api.condition.DisabledOnOs} by adding an
 * architecture check.
 *
 * @author Moritz Halbritter
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DisabledOnOsCondition.class)
public @interface DisabledOnOs {

	/**
	 * The operating systems on which the annotated class or method should be disabled.
	 * @return the operating systems where the test is disabled
	 */
	OS[] value() default {};

	/**
	 * The operating systems on which the annotated class or method should be disabled.
	 * @return the operating systems where the test is disabled
	 */
	OS[] os() default {};

	/**
	 * The architectures on which the annotated class or method should be disabled.
	 * @return the architectures where the test is disabled
	 */
	String[] architecture() default {};

	/**
	 * See {@link org.junit.jupiter.api.condition.DisabledOnOs#disabledReason()}.
	 * @return disabled reason
	 */
	String disabledReason() default "";

}

/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
 * {@link Conditional} that only matches when the specified classes are not on the
 * classpath.
 *
 * @author Dave Syer
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnClassCondition.class)
public @interface ConditionalOnMissingClass {

	/**
	 * The classes that must not be present. Since this annotation parsed by loading class
	 * bytecode it is safe to specify classes here that may ultimately not be on the
	 * classpath.
	 * @return the classes that must be present
	 * @deprecated Since 1.1.0 due to the fact that the reflection errors can occur when
	 * beans containing the annotation remain in the context. Use {@link #name()} instead.
	 */
	@Deprecated
	public Class<?>[] value() default {};

	/**
	 * The classes names that must not be present.
	 * @return the class names that must be present.
	 */
	public String[] name() default {};

}

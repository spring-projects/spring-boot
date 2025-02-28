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

package org.springframework.boot.jackson;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * Provides a mixin class implementation that registers with Jackson when using
 * {@link JsonMixinModule}.
 *
 * @author Guirong Hu
 * @see JsonMixinModule
 * @since 2.7.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JsonMixin {

	/**
	 * Alias for the {@link #type()} attribute. Allows for more concise annotation
	 * declarations e.g.: {@code @JsonMixin(MyType.class)} instead of
	 * {@code @JsonMixin(type=MyType.class)}.
	 * @return the mixed-in classes
	 * @since 2.7.0
	 */
	@AliasFor("type")
	Class<?>[] value() default {};

	/**
	 * The types that are handled by the provided mix-in class. {@link #value()} is an
	 * alias for (and mutually exclusive with) this attribute.
	 * @return the mixed-in classes
	 * @since 2.7.0
	 */
	@AliasFor("value")
	Class<?>[] type() default {};

}

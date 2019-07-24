/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.data;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * {@link Conditional} that only matches when a particular type of Spring Data repository
 * has been enabled.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@Conditional(OnRepositoryTypeCondition.class)
public @interface ConditionalOnRepositoryType {

	/**
	 * The name of the store that backs the repositories.
	 * @return the store
	 */
	String store();

	/**
	 * The required repository type.
	 * @return the required repository type
	 */
	RepositoryType type();

}

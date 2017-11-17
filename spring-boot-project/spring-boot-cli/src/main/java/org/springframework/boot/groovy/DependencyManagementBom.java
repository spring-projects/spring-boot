/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.groovy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides one or more additional sources of dependency management that is used when
 * resolving {@code @Grab} dependencies.
 *
 * @author Andy Wilkinson
 * @since 1.3.0
 */
@Target({ ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.LOCAL_VARIABLE,
		ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE })
@Retention(RetentionPolicy.SOURCE)
public @interface DependencyManagementBom {

	/**
	 * One or more sets of colon-separated coordinates ({@code group:module:version}) of a
	 * Maven bom that contains dependency management that will add to and override the
	 * default dependency management.
	 * @return the BOM coordinates
	 */
	String[] value();

}

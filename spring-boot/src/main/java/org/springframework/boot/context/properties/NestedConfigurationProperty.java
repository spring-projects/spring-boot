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

package org.springframework.boot.context.properties;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a field in a {@link ConfigurationProperties} object should be treated as
 * if it were a nested type. This annotation has no bearing on the actual binding
 * processes, but it is used by the {@code spring-boot-configuration-processor} as a hint
 * that a field is not bound as a single value. When this is specified, a nested group is
 * created for the field and its type is harvested.
 * <p>
 * This has no effect on collections and maps as these types are automatically identified.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 1.2.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NestedConfigurationProperty {

}

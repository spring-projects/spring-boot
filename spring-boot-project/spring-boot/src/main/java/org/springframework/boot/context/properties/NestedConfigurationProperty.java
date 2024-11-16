/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.context.properties;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.context.properties.bind.Nested;

/**
 * Indicates that a property in a {@link ConfigurationProperties @ConfigurationProperties}
 * object should be treated as if it were a nested type. This annotation has no bearing on
 * the actual binding processes, but it is used by the
 * {@code spring-boot-configuration-processor} as a hint that a property is not bound as a
 * single value. When this is specified, a nested group is created for the property and
 * its type is harvested.
 * <p>
 * In the example below, {@code Host} is flagged as a nested property using its field and
 * an {@code example.server.host} nested group is created with any property that
 * {@code Host} defines:<pre><code class="java">
 * &#064;ConfigurationProperties("example.server")
 * class ServerProperties {
 *
 *     &#064;NestedConfigurationProperty
 *     private final Host host = new Host();
 *
 *     public Host getHost() { ... }
 *
 *     // Other properties, getter, setter.
 *
 * }</code></pre>
 * <p>
 * The annotation can also be specified on a getter method. If you use records, you can
 * annotate the record component.
 * <p>
 * This has no effect on collections and maps as these types are automatically identified.
 * Also, the annotation is not necessary if the target type is an inner class of the
 * {@link ConfigurationProperties @ConfigurationProperties} object. In the example below,
 * {@code Host} is detected as a nested type as it is defined as an inner class:
 * <pre><code class="java">
 * &#064;ConfigurationProperties("example.server")
 * class ServerProperties {
 *
 *     private final Host host = new Host();
 *
 *     public Host getHost() { ... }
 *
 *     // Other properties, getter, setter.
 *
 *     public static class Host {
 *
 *         // properties, getter, setter.
 *
 *     }
 *
 * }</code></pre>
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Jared Bates
 * @since 1.2.0
 */
@Target({ ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Nested
public @interface NestedConfigurationProperty {

}

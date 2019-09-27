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

package org.springframework.boot.jackson;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

/**
 * {@link Component @Component} that provides {@link JsonSerializer},
 * {@link JsonDeserializer} or {@link KeyDeserializer} implementations to be registered
 * with Jackson when {@link JsonComponentModule} is in use. Can be used to annotate
 * implementations directly or a class that contains them as inner-classes. For example:
 * <pre class="code">
 * &#064;JsonComponent
 * public class CustomerJsonComponent {
 *
 *     public static class Serializer extends JsonSerializer&lt;Customer&gt; {
 *
 *         // ...
 *
 *     }
 *
 *     public static class Deserializer extends JsonDeserializer&lt;Customer&gt; {
 *
 *         // ...
 *
 *     }
 *
 * }
 *
 * </pre>
 *
 * @see JsonComponentModule
 * @since 1.4.0
 * @author Phillip Webb
 * @author Paul Aly
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface JsonComponent {

	/**
	 * The value may indicate a suggestion for a logical component name, to be turned into
	 * a Spring bean in case of an autodetected component.
	 * @return the component name
	 */
	@AliasFor(annotation = Component.class)
	String value() default "";

	/**
	 * The types that are handled by the provided serializer/deserializer. This attribute
	 * is mandatory for a {@link KeyDeserializer}, as the type cannot be inferred. For a
	 * {@link JsonSerializer} or {@link JsonDeserializer} it can be used to limit handling
	 * to a subclasses of type inferred from the generic.
	 * @return the types that should be handled by the component
	 * @since 2.2.0
	 */
	Class<?>[] type() default {};

	/**
	 * The scope under which the serializer/deserializer should be registered with the
	 * module.
	 * @return the component's handle type
	 * @since 2.2.0
	 */
	Scope scope() default Scope.VALUES;

	/**
	 * The various scopes under which a serializer/deserializer can be registered.
	 */
	enum Scope {

		/**
		 * A serializer/deserializer for regular value content.
		 * @see JsonSerializer
		 * @see JsonDeserializer
		 */
		VALUES,

		/**
		 * A serializer/deserializer for keys.
		 * @see JsonSerializer
		 * @see KeyDeserializer
		 */
		KEYS

	}

}

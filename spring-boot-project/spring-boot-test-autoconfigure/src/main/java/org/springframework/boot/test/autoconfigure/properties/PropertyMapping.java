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

package org.springframework.boot.test.autoconfigure.properties;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.TestPropertySource;

/**
 * Indicates that attributes from a test annotation should be mapped into a
 * {@link PropertySource}. Can be used at the type level, or on individual attributes. For
 * example, the following annotation declaration: <pre class="code">
 * &#064;Retention(RUNTIME)
 * &#064;PropertyMapping("my.example")
 * public &#064;interface Example {
 *
 *   String name();
 *
 * }
 * </pre> When used on a test class as follows: <pre class="code">
 * &#064;Example(name="Spring")
 * public class MyTest {
 * }
 * </pre> will result in a {@literal my.example.name} property being added with the value
 * {@literal "Spring"}.
 * <p>
 *
 * @author Phillip Webb
 * @since 1.4.0
 * @see AnnotationsPropertySource
 * @see TestPropertySource
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
public @interface PropertyMapping {

	/**
	 * Defines the property mapping. When used at the type-level, this value will be used
	 * as a prefix for all mapped attributes. When used on an attribute, the value
	 * overrides the generated (kebab case) name.
	 * @return the property mapping
	 */
	String value() default "";

	/**
	 * Determines if mapping should be skipped. When specified at the type-level indicates
	 * if skipping should occur by default or not. When used at the attribute-level,
	 * overrides the type-level default.
	 * @return if mapping should be skipped
	 */
	SkipPropertyMapping skip() default SkipPropertyMapping.NO;

}

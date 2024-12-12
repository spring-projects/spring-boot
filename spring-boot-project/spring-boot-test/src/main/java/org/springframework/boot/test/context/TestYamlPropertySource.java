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

package org.springframework.boot.test.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.TestPropertySource;

/**
 * {@code @TestYamlPropertySource} is an annotation that can be applied to a test class to
 * configure the locations of YAML files and inlined properties to be added to the
 * Environment's set of PropertySources for an ApplicationContext for integration tests.
 * <p>
 * Provides a convenient alternative for
 * {@code @TestPropertySource(locations = "...", factory = YamlPropertySourceFactory.class)}.
 * <p>
 * {@code @TestYamlPropertySource} should be considered as {@code @TestPropertySource} but
 * for YAML files. It intentionally does not support multi-document YAML files to maintain
 * consistency with the behavior of {@code @TestPropertySource}.
 *
 * @author Dmytro Nosan
 * @since 3.5.0
 * @see YamlPropertySourceFactory
 * @see TestPropertySource
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@TestPropertySource(factory = YamlPropertySourceFactory.class)
@Repeatable(TestYamlPropertySources.class)
public @interface TestYamlPropertySource {

	/**
	 * Alias for {@link TestPropertySource#value()}.
	 * @return The resource locations of YAML files.
	 * @see TestPropertySource#value() for more details.
	 */
	@AliasFor(attribute = "value", annotation = TestPropertySource.class)
	String[] value() default {};

	/**
	 * Alias for {@link TestPropertySource#locations()}.
	 * @return The resource locations of YAML files.
	 * @see TestPropertySource#locations() for more details.
	 */
	@AliasFor(attribute = "locations", annotation = TestPropertySource.class)
	String[] locations() default {};

	/**
	 * Alias for {@link TestPropertySource#inheritLocations()}.
	 * @return Whether test property source {@link #locations} from superclasses and
	 * enclosing classes should be <em>inherited</em>.
	 * @see TestPropertySource#inheritLocations() for more details.
	 */
	@AliasFor(attribute = "inheritLocations", annotation = TestPropertySource.class)
	boolean inheritLocations() default true;

	/**
	 * Alias for {@link TestPropertySource#properties()}.
	 * @return <em>Inlined properties</em> in the form of <em>key-value</em> pairs that
	 * should be added to the Environment
	 * @see TestPropertySource#properties() for more details.
	 */
	@AliasFor(attribute = "properties", annotation = TestPropertySource.class)
	String[] properties() default {};

	/**
	 * Alias for {@link TestPropertySource#inheritProperties()}.
	 * @return Whether inlined test {@link #properties} from superclasses and enclosing
	 * classes should be <em>inherited</em>.
	 * @see TestPropertySource#inheritProperties() for more details.
	 */
	@AliasFor(attribute = "inheritProperties", annotation = TestPropertySource.class)
	boolean inheritProperties() default true;

}

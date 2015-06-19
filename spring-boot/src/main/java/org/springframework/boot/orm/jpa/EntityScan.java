/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.orm.jpa;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

/**
 * Configures the {@link LocalContainerEntityManagerFactoryBean} to to scan for entity
 * classes in the classpath. This annotation provides an alternative to manually setting
 * {@link LocalContainerEntityManagerFactoryBean#setPackagesToScan(String...)} and is
 * particularly useful if you want to configure entity scanning in a type-safe way, or if
 * your {@link LocalContainerEntityManagerFactoryBean} is auto-configured.
 * <p>
 * A {@link LocalContainerEntityManagerFactoryBean} must be configured within your Spring
 * ApplicationContext in order to use entity scanning. Furthermore, any existing
 * {@code packagesToScan} setting will be replaced.
 * <p>
 * One of {@link #basePackageClasses()}, {@link #basePackages()} or its alias
 * {@link #value()} may be specified to define specific packages to scan. If specific
 * packages are not defined scanning will occur from the package of the class with this
 * annotation.
 *
 * @author Phillip Webb
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(EntityScanRegistrar.class)
public @interface EntityScan {

	/**
	 * Alias for the {@link #basePackages()} attribute. Allows for more concise annotation
	 * declarations e.g.: {@code @EntityScan("org.my.pkg")} instead of
	 * {@code @EntityScan(basePackages="org.my.pkg")}.
	 * @return the base packages to scan
	 */
	String[] value() default {};

	/**
	 * Base packages to scan for annotated entities. {@link #value()} is an alias for (and
	 * mutually exclusive with) this attribute.
	 * <p>
	 * Use {@link #basePackageClasses()} for a type-safe alternative to String-based
	 * package names.
	 * @return the base packages to scan
	 */
	String[] basePackages() default {};

	/**
	 * Type-safe alternative to {@link #basePackages()} for specifying the packages to
	 * scan for annotated entities. The package of each class specified will be scanned.
	 * <p>
	 * Consider creating a special no-op marker class or interface in each package that
	 * serves no purpose other than being referenced by this attribute.
	 * @return classes form the base packages to scan
	 */
	Class<?>[] basePackageClasses() default {};

}

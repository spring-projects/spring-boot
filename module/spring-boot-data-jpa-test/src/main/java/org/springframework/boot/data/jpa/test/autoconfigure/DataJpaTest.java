/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.data.jpa.test.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureJdbc;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.AutoConfigureTestEntityManager;
import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration;
import org.springframework.boot.test.context.PropertyMapping;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.filter.annotation.TypeExcludeFilters;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.env.Environment;
import org.springframework.data.repository.config.BootstrapMode;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * Annotation for a JPA test that focuses <strong>only</strong> on JPA components.
 * <p>
 * Using this annotation only enables auto-configuration that is relevant to Data JPA
 * tests. Similarly, component scanning is limited to JPA repositories and entities
 * ({@code @Entity}).
 * <p>
 * By default, tests annotated with {@code @DataJpaTest} are transactional and roll back
 * at the end of each test. They also use an embedded in-memory database (replacing any
 * explicit or usually auto-configured DataSource). The
 * {@link AutoConfigureTestDatabase @AutoConfigureTestDatabase} annotation can be used to
 * override these settings.
 * <p>
 * SQL queries are logged by default by setting the {@code spring.jpa.show-sql} property
 * to {@code true}. This can be disabled using the {@link DataJpaTest#showSql() showSql}
 * attribute.
 * <p>
 * If you are looking to load your full application configuration, but use an embedded
 * database, you should consider {@link SpringBootTest @SpringBootTest} combined with
 * {@link AutoConfigureTestDatabase @AutoConfigureTestDatabase} rather than this
 * annotation.
 * <p>
 * When using JUnit 4, this annotation should be used in combination with
 * {@code @RunWith(SpringRunner.class)}.
 *
 * @author Phillip Webb
 * @author Artsiom Yudovin
 * @author Scott Frederick
 * @since 4.0.0
 * @see AutoConfigureDataJpa
 * @see AutoConfigureTestDatabase
 * @see AutoConfigureTestEntityManager
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@BootstrapWith(DataJpaTestContextBootstrapper.class)
@ExtendWith(SpringExtension.class)
@OverrideAutoConfiguration(enabled = false)
@TypeExcludeFilters(DataJpaTypeExcludeFilter.class)
@Transactional
@AutoConfigureDataJpa
@AutoConfigureJdbc
@AutoConfigureTestDatabase
@AutoConfigureTestEntityManager
@ImportAutoConfiguration
public @interface DataJpaTest {

	/**
	 * Properties in form {@literal key=value} that should be added to the Spring
	 * {@link Environment} before the test runs.
	 * @return the properties to add
	 * @since 2.1.0
	 */
	String[] properties() default {};

	/**
	 * If SQL output should be logged.
	 * @return if SQL is logged
	 */
	@PropertyMapping("spring.jpa.show-sql")
	boolean showSql() default true;

	/**
	 * The {@link BootstrapMode} for the test repository support. Defaults to
	 * {@link BootstrapMode#DEFAULT}.
	 * @return the {@link BootstrapMode} to use for testing the repository
	 */
	@PropertyMapping("spring.data.jpa.repositories.bootstrap-mode")
	BootstrapMode bootstrapMode() default BootstrapMode.DEFAULT;

	/**
	 * Determines if default filtering should be used with
	 * {@link SpringBootApplication @SpringBootApplication}. By default no beans are
	 * included.
	 * @see #includeFilters()
	 * @see #excludeFilters()
	 * @return if default filters should be used
	 */
	boolean useDefaultFilters() default true;

	/**
	 * A set of include filters which can be used to add otherwise filtered beans to the
	 * application context.
	 * @return include filters to apply
	 */
	Filter[] includeFilters() default {};

	/**
	 * A set of exclude filters which can be used to filter beans that would otherwise be
	 * added to the application context.
	 * @return exclude filters to apply
	 */
	Filter[] excludeFilters() default {};

	/**
	 * Auto-configuration exclusions that should be applied for this test.
	 * @return auto-configuration exclusions to apply
	 */
	@AliasFor(annotation = ImportAutoConfiguration.class, attribute = "exclude")
	Class<?>[] excludeAutoConfiguration() default {};

}

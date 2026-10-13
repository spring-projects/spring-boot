/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.flyway.autoconfigure;

import org.flywaydb.core.Flyway;

/**
 * Strategy used to initialize {@link Flyway} migration. Custom implementations may be
 * registered as a {@code @Bean} to override the default migration behavior.
 *
 * @author Andreas Ahlenstorf
 * @author Phillip Webb
 * @since 4.0.0
 */
@FunctionalInterface
public interface FlywayMigrationStrategy {

	/**
	 * Trigger flyway migration.
	 * @param flyway the flyway instance
	 */
	void migrate(Flyway flyway);

}

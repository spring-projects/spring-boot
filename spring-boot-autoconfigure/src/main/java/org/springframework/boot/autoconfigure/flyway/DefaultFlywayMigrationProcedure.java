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

package org.springframework.boot.autoconfigure.flyway;

import org.flywaydb.core.Flyway;

/**
 * {@link FlywayMigrationProcedure} that just executes {@link Flyway#migrate()} on
 * application startup to bring the database up-to-date. It is used by default if no other
 * bean implementing {@link FlywayMigrationProcedure} is registered with the application
 * context.
 *
 * @author Andreas Ahlenstorf
 */
final class DefaultFlywayMigrationProcedure implements FlywayMigrationProcedure {

	@Override
	public void apply(Flyway flyway) {
		flyway.migrate();
	}

}

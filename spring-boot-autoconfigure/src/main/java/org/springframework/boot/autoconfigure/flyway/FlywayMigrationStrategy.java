/*
 * Copyright 2012-2015 the original author or authors.
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

import org.apache.commons.lang.BooleanUtils;
import org.flywaydb.core.Flyway;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

/**
 * Strategy used to initialize {@link Flyway} migration. Custom implementations may be
 * registered as a {@code @Bean} to override the default migration behavior.
 *
 * @author Andreas Ahlenstorf
 * @author Phillip Webb
 */
public class FlywayMigrationStrategy implements EnvironmentAware {

    private Environment environment;

    public void migrate(Flyway flyway) {
        if (BooleanUtils.isNotFalse(environment.getProperty("flyway.migrateOnInit", Boolean.class))) {
            flyway.migrate();
        }
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}

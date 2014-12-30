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

package org.springframework.boot.actuate.health;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.data.cassandra.core.CassandraAdminOperations;
import org.springframework.util.Assert;

/**
 * Simple implementation of a {@link HealthIndicator} returning status information for
 * Cassandra data stores.
 *
 * @author Julien Dubois
 * @since 1.3.0
 */
public class CassandraHealthIndicator extends AbstractHealthIndicator {

    private static Log logger = LogFactory.getLog(CassandraHealthIndicator.class);

    private CassandraAdminOperations cassandraTemplate;

    public CassandraHealthIndicator(CassandraAdminOperations cassandraTemplate) {
        Assert.notNull(cassandraTemplate, "cassandraTemplate must not be null");
        this.cassandraTemplate = cassandraTemplate;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        logger.debug("Initializing Cassandra health indicator");
        try {
            Select select = QueryBuilder.select("release_version").from("system", "local");
            ResultSet results = cassandraTemplate.query(select);
            if (results.isExhausted()) {
                builder.up();
            } else {
                builder.up().withDetail("version", results.one().getString(0));
            }
        } catch (Exception e) {
            logger.debug("Cannot connect to Cassandra cluster. Error: {}", e);
            builder.down(e);
        }
    }
}

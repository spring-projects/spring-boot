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

package org.springframework.boot.autoconfigure.neo4j;

import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.service.Components;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Neo4j.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Josh Long
 * @author Andy Wilkinson
 * @author Eddú Meléndez
 * @author Michael Hunger
 * @author Vince Bickers
 */
@ConfigurationProperties(prefix = "spring.data.neo4j")
public class Neo4jProperties {

    // if you don't set this up somewhere, this is what we'll use by default
    private String driver = "org.neo4j.ogm.drivers.embedded.driver.EmbeddedDriver";

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public Configuration configure() {
        Configuration configuration = new Configuration();
        configuration.driverConfiguration()
                .setDriverClassName( getDriver() );
        Components.configure( configuration );
        return configuration;
    }
}

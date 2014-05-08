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
package org.springframework.boot.autoconfigure.jdbc;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests for {@link DriverClassNameProvider}.
 *
 * @author Maciej Walkowiak
 */
public class DriverClassNameProviderTest {
    private DriverClassNameProvider driverClassNameProvider = new DriverClassNameProvider();

    @Test
    public void testGettingClassNameForKnownDatabase() {
        String driverClassName = driverClassNameProvider.getDriverClassName("jdbc:postgresql://hostname/dbname");

        assertEquals("org.postgresql.Driver", driverClassName);
    }

    @Test
    public void testReturnsNullForUnknownDatabase() {
        String driverClassName = driverClassNameProvider.getDriverClassName("jdbc:unknowndb://hostname/dbname");

        assertNull(driverClassName);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFailureOnNullJdbcUrl() {
        driverClassNameProvider.getDriverClassName(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFailureOnMalformedJdbcUrl() {
        driverClassNameProvider.getDriverClassName("malformed:url");
    }
}
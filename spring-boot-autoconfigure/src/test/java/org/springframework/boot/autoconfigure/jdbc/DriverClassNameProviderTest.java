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
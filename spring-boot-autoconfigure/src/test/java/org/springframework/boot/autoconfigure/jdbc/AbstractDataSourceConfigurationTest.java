package org.springframework.boot.autoconfigure.jdbc;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link AbstractDataSourceConfiguration}.
 *
 * @author Maciej Walkowiak
 */
public class AbstractDataSourceConfigurationTest {
    @Test
    public void testGettingCorrectDriverClassNameFromJdbcUrlWhenDriverClassNameNotDefined() {
        AbstractDataSourceConfiguration configuration = new AbstractDataSourceConfiguration() {};
        configuration.setUrl("jdbc:mysql://mydb");

        String driverClassName = configuration.getDriverClassName();

        assertEquals(driverClassName, "com.mysql.jdbc.Driver");
    }

    @Test
    public void testGettingDriverClassNameFromDriverClassNamePropertyWhenDefined() {
        AbstractDataSourceConfiguration configuration = new AbstractDataSourceConfiguration() {};
        configuration.setUrl("jdbc:mysql://mydb");
        configuration.setDriverClassName("my.driver.ClassName");

        String driverClassName = configuration.getDriverClassName();

        assertEquals(driverClassName, "my.driver.ClassName");
    }

}

package org.springframework.boot.autoconfigure.jdbc;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link DataSourceProperties}.
 *
 * @author Maciej Walkowiak
 */
public class DataSourcePropertiesTests {

	@Test
	public void correctDriverClassNameFromJdbcUrlWhenDriverClassNameNotDefined() {
		DataSourceProperties configuration = new DataSourceProperties();
		configuration.setUrl("jdbc:mysql://mydb");
		String driverClassName = configuration.getDriverClassName();
		assertEquals(driverClassName, "com.mysql.jdbc.Driver");
	}

	@Test
	public void driverClassNameFromDriverClassNamePropertyWhenDefined() {
		DataSourceProperties configuration = new DataSourceProperties();
		configuration.setUrl("jdbc:mysql://mydb");
		configuration.setDriverClassName("my.driver.ClassName");
		String driverClassName = configuration.getDriverClassName();
		assertEquals(driverClassName, "my.driver.ClassName");
	}

}

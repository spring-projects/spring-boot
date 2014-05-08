package org.springframework.boot.autoconfigure.jdbc;

import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides JDBC driver class name for given JDBC URL.
 *
 * @author Maciej Walkowiak
 * @since 1.1.0
 */
class DriverClassNameProvider {
    private static final String JDBC_URL_PREFIX = "jdbc:";
    private static final Map<String, String> driverMap = new HashMap<String, String>() {{
        put("mysql","com.mysql.jdbc.Driver");
        put("postgresql","org.postgresql.Driver");
        put("h2","org.h2.Driver");
    }};

    /**
     * Used to find JDBC driver class name based on given JDBC URL
     *
     * @param jdbcUrl JDBC URL
     * @return driver class name or null if not found
     */
    String getDriverClassName(final String jdbcUrl) {
        Assert.notNull(jdbcUrl, "JDBC URL cannot be null");

        if (!jdbcUrl.startsWith(JDBC_URL_PREFIX)) {
            throw new IllegalArgumentException("JDBC URL should start with '" + JDBC_URL_PREFIX + "'");
        }

        String urlWithoutPrefix = jdbcUrl.substring(JDBC_URL_PREFIX.length());
        String result = null;

        for (Map.Entry<String, String> driver : driverMap.entrySet()) {
            if (urlWithoutPrefix.startsWith(driver.getKey())) {
                result = driver.getValue();

                break;
            }
        }

        return result;
    }
}

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
    private static final String JDBC_URL_PREFIX = "jdbc";
    private static final Map<String, String> driverMap = new HashMap<String, String>() {{
        put("db2","com.ibm.db2.jcc.DB2Driver");
        put("derby","org.apache.derby.jdbc.EmbeddedDriver");
        put("h2","org.h2.Driver");
        put("hsqldb","org.hsqldb.jdbcDriver");
        put("sqlite","org.sqlite.JDBC");
        put("mysql","com.mysql.jdbc.Driver");
        put("mariadb","org.mariadb.jdbc.Driver");
        put("google","com.google.appengine.api.rdbms.AppEngineDriver");
        put("oracle","oracle.jdbc.OracleDriver");
        put("postgresql","org.postgresql.Driver");
        put("jtds","net.sourceforge.jtds.jdbc.Driver");
        put("sqlserver","com.microsoft.sqlserver.jdbc.SQLServerDriver");

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
            if (urlWithoutPrefix.startsWith(":" + driver.getKey() + ":")) {
                result = driver.getValue();

                break;
            }
        }

        return result;
    }
}

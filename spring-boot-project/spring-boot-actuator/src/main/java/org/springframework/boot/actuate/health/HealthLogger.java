package org.springframework.boot.actuate.health;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

public class HealthLogger {

    private static final Log logger = LogFactory.getLog(HealthLogger.class);
    private static final String DEFAULT_MESSAGE = "Health check failed";

    public static void logExceptionIfPresent(Throwable throwable) {
        if (throwable != null && logger.isWarnEnabled()) {
            String message = (throwable instanceof Exception ex) ? ex.getMessage() : null;
            logger.warn(StringUtils.hasText(message) ? message : DEFAULT_MESSAGE, throwable);
        }
    }
}

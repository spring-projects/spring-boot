package org.springframework.boot.liquibase;

import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.logging.core.AbstractLogger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;

/**
 * {@link AbstractLogger} that replaces the liquibase {@link liquibase.logging.core.DefaultLogger} with a
 * version that uses spring logging.
 *
 * @author Michael Cramer
 */
public class SpringLiquibaseLogger extends AbstractLogger {
    public static final int PRIORITY = 10;
    private String changeLogName = null;
    private String changeSetName = null;
    private Log logger;

    @Override
    public void setName(String name) {
        this.logger = LogFactory.getLog(name);
    }

    @Override
    public void setLogLevel(String logLevel, String logFile) {
        super.setLogLevel(logLevel);
    }

    @Override
    public void severe(String message) {
        if (logger.isErrorEnabled() && StringUtils.hasLength(message)) {
            logger.error(buildMessage(message));
        }
    }

    @Override
    public void severe(String message, Throwable e) {
        if (logger.isErrorEnabled() && StringUtils.hasLength(message)) {
            logger.error(buildMessage(message), e);
        }
    }

    @Override
    public void warning(String message) {
        if (logger.isWarnEnabled() && StringUtils.hasLength(message)) {
            logger.warn(buildMessage(message));
        }
    }

    @Override
    public void warning(String message, Throwable e) {
        if (logger.isWarnEnabled() && StringUtils.hasLength(message)) {
            logger.warn(buildMessage(message), e);
        }
    }

    @Override
    public void info(String message) {
        if (logger.isInfoEnabled() && StringUtils.hasLength(message)) {
            logger.info(buildMessage(message));
        }
    }

    @Override
    public void info(String message, Throwable e) {
        if (logger.isWarnEnabled() && StringUtils.hasLength(message)) {
            logger.info(buildMessage(message), e);
        }
    }

    @Override
    public void debug(String message) {
        if (logger.isDebugEnabled() && StringUtils.hasLength(message)) {
            logger.debug(buildMessage(message));
        }
    }

    @Override
    public void debug(String message, Throwable e) {
        if (logger.isDebugEnabled() && StringUtils.hasLength(message)) {
            logger.debug(buildMessage(message), e);
        }
    }

    @Override
    public void setChangeLog(DatabaseChangeLog databaseChangeLog) {
        changeLogName = databaseChangeLog == null ? null : databaseChangeLog.getFilePath();
    }

    @Override
    public void setChangeSet(ChangeSet changeSet) {
        changeSetName = changeSet == null ? null : changeSet.toString(false);
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    private String buildMessage(String message) {
        Collection<String> description = new ArrayList<String>();
        if (changeLogName != null) {
            description.add(changeLogName);
        }
        if (changeSetName != null) {
            description.add(changeSetName.replace(changeLogName + "::", ""));
        }
        description.add(message);
        return StringUtils.collectionToDelimitedString(description, ": ");
    }
}

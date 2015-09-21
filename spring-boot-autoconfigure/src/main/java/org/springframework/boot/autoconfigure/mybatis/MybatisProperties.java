package org.springframework.boot.autoconfigure.mybatis;

import org.apache.ibatis.session.ExecutorType;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Mybatis.
 *
 * @author Eddú Meléndez
 * @since 1.3.0
 */
@ConfigurationProperties(prefix = MybatisProperties.SPRING_MYBATIS_PREFIX)
public class MybatisProperties {

	public static final String SPRING_MYBATIS_PREFIX = "spring.mybatis";

	/**
	 * Config file path.
	 */
	private String config;

	/**
	 * Check the config file exists.
	 */
	private boolean checkConfigLocation = false;

	/**
	 * Execution mode.
	 */
	private ExecutorType executorType = ExecutorType.SIMPLE;

	public String getConfig() {
		return this.config;
	}

	public void setConfig(String config) {
		this.config = config;
	}

	public boolean isCheckConfigLocation() {
		return this.checkConfigLocation;
	}

	public void setCheckConfigLocation(boolean checkConfigLocation) {
		this.checkConfigLocation = checkConfigLocation;
	}

	public ExecutorType getExecutorType() {
		return this.executorType;
	}

	public void setExecutorType(ExecutorType executorType) {
		this.executorType = executorType;
	}
}

/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.metrics.dropwizard.annotation;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Dropwizard application metrics reporter.
 *
 * @author Sergey Kuptsov
 */
@ConfigurationProperties(prefix = "spring.metrics.annotation")
public class DropwizardMetricsAnnotationsProperties {

	/**
	 * If true creates a HealthCheckRegistry bean.
	 */
	private boolean healthCheck;

	/**
	 * If set tries to find and use given metricsRegistryBeanName bean as metrics registry
	 * If not set or bean not found - used default metrics registry.
	 */
	private String metricsRegistryBeanName;

	/**
	 * Reporters definition.
	 */
	private final Reporter reporter = new Reporter();

	public boolean isHealthCheck() {
		return this.healthCheck;
	}

	public void setHealthCheck(boolean healthCheck) {
		this.healthCheck = healthCheck;
	}

	public Reporter getReporter() {
		return this.reporter;
	}

	public String getMetricsRegistryBeanName() {
		return this.metricsRegistryBeanName;
	}

	public void setMetricsRegistryBeanName(String metricsRegistryBeanName) {
		this.metricsRegistryBeanName = metricsRegistryBeanName;
	}

	public static class Reporter {
		private Jmx jmx = new Jmx();
		private Console console = new Console();
		private Csv csv = new Csv();
		private Slf4j slf4j = new Slf4j();

		public Jmx getJmx() {
			return this.jmx;
		}

		public void setJmx(Jmx jmx) {
			this.jmx = jmx;
		}

		public Console getConsole() {
			return this.console;
		}

		public void setConsole(Console console) {
			this.console = console;
		}

		public Csv getCsv() {
			return this.csv;
		}

		public void setCsv(Csv csv) {
			this.csv = csv;
		}

		public Slf4j getSlf4j() {
			return this.slf4j;
		}

		public void setSlf4j(Slf4j slf4j) {
			this.slf4j = slf4j;
		}

		public static class Jmx extends BaseReporter {
		}

		public static class Console extends BaseScheduledReporter {
		}

		public static class Csv extends BaseScheduledReporter {
			private File file;

			public File getFile() {
				return this.file;
			}

			public void setFile(File file) {
				this.file = file;
			}
		}

		public static class Slf4j extends BaseScheduledReporter {
		}

		public static class BaseScheduledReporter extends BaseReporter {
			private int periodSec = 60;

			public int getPeriodSec() {
				return this.periodSec;
			}

			public void setPeriodSec(int periodSec) {
				this.periodSec = periodSec;
			}

			public TimeUnit getTimeUnit() {
				return TimeUnit.SECONDS;
			}
		}

		public static abstract class BaseReporter {
			private boolean enabled;

			public boolean isEnabled() {
				return this.enabled;
			}

			public void setEnabled(boolean enabled) {
				this.enabled = enabled;
			}
		}
	}
}

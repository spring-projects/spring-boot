/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.specific;

import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * Sample config for enum and default values.
 *
 * @author Stephane Nicoll
 */
@TestConfigurationProperties("test")
public class EnumValuesPojo {

	private ChronoUnit seconds = ChronoUnit.SECONDS;

	private ChronoField hourOfDay = ChronoField.HOUR_OF_DAY;

	public ChronoUnit getSeconds() {
		return this.seconds;
	}

	public void setSeconds(ChronoUnit seconds) {
		this.seconds = seconds;
	}

	public ChronoField getHourOfDay() {
		return this.hourOfDay;
	}

	public void setHourOfDay(ChronoField hourOfDay) {
		this.hourOfDay = hourOfDay;
	}

}

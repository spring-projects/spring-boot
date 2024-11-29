/*
 * Copyright 2012-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.configurationsample.specific;

import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;

import org.springframework.boot.configurationsample.ConfigurationProperties;

/**
 * Sample config for enum and default values.
 *
 * @author Stephane Nicoll
 */
@ConfigurationProperties("test")
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

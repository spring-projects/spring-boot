/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.configurationsample.fieldvalues;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Period;
import java.time.temporal.ChronoUnit;

import org.springframework.boot.configurationsample.TestConfigurationProperties;
import org.springframework.util.MimeType;
import org.springframework.util.unit.DataSize;

/**
 * Sample object containing fields with initial values.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
@SuppressWarnings("unused")
@TestConfigurationProperties
public class FieldValues {

	private static final String STRING_CONST = "c";

	private static final boolean BOOLEAN_CONST = true;

	private static final Boolean BOOLEAN_OBJ_CONST = true;

	private static final int INTEGER_CONST = 2;

	private static final Integer INTEGER_OBJ_CONST = 4;

	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	private static final MimeType DEFAULT_MIME_TYPE = MimeType.valueOf("text/plain");

	private static final String[] STRING_ARRAY_CONST = new String[] { "OK", "KO" };

	private String string = "1";

	private String stringNone;

	private String stringConst = STRING_CONST;

	private boolean bool = true;

	private boolean boolNone;

	private boolean boolConst = BOOLEAN_CONST;

	private boolean boolInverted = !false;

	private Boolean boolObject = Boolean.TRUE;

	private Boolean boolObjectNone;

	private Boolean boolObjectConst = BOOLEAN_OBJ_CONST;

	private int integer = 1;

	private int integerNone;

	private int integerConst = INTEGER_CONST;

	private int integerArithmetic = 10 * 10;

	private int integerMax = Math.max(10, 20);

	private Integer integerObject = 3;

	private Integer integerObjectNone;

	private Integer integerObjectConst = INTEGER_OBJ_CONST;

	private Charset charset = StandardCharsets.US_ASCII;

	private Charset charsetConst = DEFAULT_CHARSET;

	private MimeType mimeType = MimeType.valueOf("text/html");

	private MimeType mimeTypeConst = DEFAULT_MIME_TYPE;

	private Object object = 123;

	private Object objectNone;

	private Object objectConst = STRING_CONST;

	private Object objectInstance = new StringBuffer();

	private String[] stringArray = new String[] { "FOO", "BAR" };

	private String[] stringArrayNone;

	private String[] stringEmptyArray = new String[0];

	private String[] stringArrayConst = STRING_ARRAY_CONST;

	private String[] stringArrayConstElements = new String[] { STRING_CONST };

	private Integer[] integerArray = new Integer[] { 42, 24 };

	private int[] intArrayUnsupportedExpression = new int[] { 10 + 10 };

	private UnknownElementType[] unknownArray = new UnknownElementType[] { new UnknownElementType() };

	private Duration durationNone;

	private Duration durationNanos = Duration.ofNanos(5);

	private Duration durationMillis = Duration.ofMillis(10);

	private Duration durationSeconds = Duration.ofSeconds(20);

	private Duration durationMinutes = Duration.ofMinutes(30);

	private Duration durationHours = Duration.ofHours(40);

	private Duration durationDays = Duration.ofDays(50);

	private Duration durationZero = Duration.ZERO;

	private DataSize dataSizeNone;

	private DataSize dataSizeBytes = DataSize.ofBytes(5);

	private DataSize dataSizeKilobytes = DataSize.ofKilobytes(10);

	private DataSize dataSizeMegabytes = DataSize.ofMegabytes(20);

	private DataSize dataSizeGigabytes = DataSize.ofGigabytes(30);

	private DataSize dataSizeTerabytes = DataSize.ofTerabytes(40);

	private Period periodNone;

	private Period periodDays = Period.ofDays(3);

	private Period periodWeeks = Period.ofWeeks(2);

	private Period periodMonths = Period.ofMonths(10);

	private Period periodYears = Period.ofYears(15);

	private Period periodZero = Period.ZERO;

	private ChronoUnit enumNone;

	private ChronoUnit enumSimple = ChronoUnit.SECONDS;

	private java.time.temporal.ChronoField enumQualified = java.time.temporal.ChronoField.HOUR_OF_DAY;

	private ChronoUnit enumWithIndirection = SampleOptions.DEFAULT_UNIT;

	private int memberSelectInt = SampleOptions.DEFAULT_MAX_RETRIES;

	public static class SampleOptions {

		static final Integer DEFAULT_MAX_RETRIES = 20;

		static final ChronoUnit DEFAULT_UNIT = ChronoUnit.SECONDS;

	}

}

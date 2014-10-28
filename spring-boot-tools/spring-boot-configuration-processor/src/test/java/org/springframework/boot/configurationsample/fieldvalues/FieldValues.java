/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.configurationsample.fieldvalues;

import org.springframework.boot.configurationsample.ConfigurationProperties;

/**
 * Sample object containing fields with initial values.
 *
 * @author Phillip Webb
 */
@SuppressWarnings("unused")
@ConfigurationProperties
public class FieldValues {

	private static final String STRING_CONST = "c";

	private static final boolean BOOLEAN_CONST = true;

	private static final Boolean BOOLEAN_OBJ_CONST = true;

	private static final int INTEGER_CONST = 2;

	private static final Integer INTEGER_OBJ_CONST = 4;

	private String string = "1";

	private String stringNone;

	private String stringConst = STRING_CONST;

	private boolean bool = true;

	private boolean boolNone;

	private boolean boolConst = BOOLEAN_CONST;

	private Boolean boolObject = Boolean.TRUE;

	private Boolean boolObjectNone;

	private Boolean boolObjectConst = BOOLEAN_OBJ_CONST;

	private int integer = 1;

	private int integerNone;

	private int integerConst = INTEGER_CONST;

	private Integer integerObject = 3;

	private Integer integerObjectNone;

	private Integer integerObjectConst = INTEGER_OBJ_CONST;

	private Object object = 123;

	private Object objectNone;

	private Object objectConst = STRING_CONST;

	private Object objectInstance = new StringBuffer();

}

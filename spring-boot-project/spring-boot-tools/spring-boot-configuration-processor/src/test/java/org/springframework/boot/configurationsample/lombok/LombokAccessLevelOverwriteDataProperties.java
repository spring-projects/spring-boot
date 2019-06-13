/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.configurationsample.lombok;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.configurationsample.ConfigurationProperties;

/**
 * Configuration properties using Lombok @Data on element level and overwriting behaviour
 * with @Getter and @Setter at field level.
 *
 * @author Jonas Keßler
 */
@Data
@ConfigurationProperties(prefix = "accesslevel.overwrite.data")
@SuppressWarnings("unused")
public class LombokAccessLevelOverwriteDataProperties {

	private String name0;

	@Getter(AccessLevel.PUBLIC)
	@Setter(AccessLevel.PUBLIC)
	private String name1;

	@Getter(AccessLevel.PUBLIC)
	private String name2;

	@Setter(AccessLevel.PUBLIC)
	private String name3;

	@Getter
	@Setter
	private String name4;

	@Getter
	private String name5;

	@Setter
	private String name6;

	/*
	 * AccessLevel.NONE
	 */
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private String ignoredAccessLevelNone;

	@Getter(AccessLevel.NONE)
	private String ignoredGetterAccessLevelNone;

	@Setter(AccessLevel.NONE)
	private String ignoredSetterAccessLevelNone;

	/*
	 * AccessLevel.PRIVATE
	 */
	@Getter(AccessLevel.PRIVATE)
	@Setter(AccessLevel.PRIVATE)
	private String ignoredAccessLevelPrivate;

	@Getter(AccessLevel.PRIVATE)
	private String ignoredGetterAccessLevelPrivate;

	@Setter(AccessLevel.PRIVATE)
	private String ignoredSetterAccessLevelPrivate;

	/*
	 * AccessLevel.PACKAGE
	 */
	@Getter(AccessLevel.PACKAGE)
	@Setter(AccessLevel.PACKAGE)
	private String ignoredAccessLevelPackage;

	@Getter(AccessLevel.PACKAGE)
	private String ignoredGetterAccessLevelPackage;

	@Setter(AccessLevel.PACKAGE)
	private String ignoredSetterAccessLevelPackage;

	/*
	 * AccessLevel.PROTECTED
	 */
	@Getter(AccessLevel.PROTECTED)
	@Setter(AccessLevel.PROTECTED)
	private String ignoredAccessLevelProtected;

	@Getter(AccessLevel.PROTECTED)
	private String ignoredGetterAccessLevelProtected;

	@Setter(AccessLevel.PROTECTED)
	private String ignoredSetterAccessLevelProtected;

	/*
	 * AccessLevel.MODULE
	 */
	@Getter(AccessLevel.MODULE)
	@Setter(AccessLevel.MODULE)
	private String ignoredAccessLevelModule;

	@Getter(AccessLevel.MODULE)
	private String ignoredGetterAccessLevelModule;

	@Setter(AccessLevel.MODULE)
	private String ignoredSetterAccessLevelModule;

}

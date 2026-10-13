/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.lombok;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * Configuration properties using Lombok @Data on element level and overwriting behaviour
 * with @Getter and @Setter at field level.
 *
 * @author Jonas Keßler
 */
@Data
@TestConfigurationProperties("accesslevel.overwrite.data")
@SuppressWarnings({ "unused", "deprecation" })
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

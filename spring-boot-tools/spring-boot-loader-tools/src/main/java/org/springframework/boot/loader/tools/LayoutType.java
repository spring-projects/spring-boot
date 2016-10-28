/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.loader.tools;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.support.SpringFactoriesLoader;

/**
 * Archive layout types.
 */
public enum LayoutType {

	/**
	 * Jar Layout.
	 */
	JAR(new Layouts.Jar()),

	/**
	 * War Layout.
	 */
	WAR(new Layouts.War()),

	/**
	 * Zip Layout.
	 */
	ZIP(new Layouts.Expanded()),

	/**
	 * Dir Layout.
	 */
	DIR(new Layouts.Expanded()),

	/**
	 * Module Layout.
	 */
	MODULE(new Layouts.Module()),

	/**
	 * No Layout.
	 */
	NONE(new Layouts.None());

	private static Map<String, Layout> customTypes;

	private final Layout layout;

	public Layout layout() {
		return this.layout;
	}

	LayoutType(Layout layout) {
		this.layout = layout;
	}

	public static Layout layout(String value) {
		try {
			return valueOf(value).layout();
		}
		catch (IllegalArgumentException e) {
			if (customTypes == null) {
				customTypes = new HashMap<String, Layout>();
				lookupCustomTypes();
			}
			Layout layout = customTypes.get(value);
			if (layout == null) {
				throw new IllegalArgumentException(
						"Cannot resolve custom layout type: " + value);
			}
			return layout;
		}
	}

	private static void lookupCustomTypes() {
		ClassLoader classLoader = LayoutType.class.getClassLoader();
		List<LayoutFactory> factories = SpringFactoriesLoader
				.loadFactories(LayoutFactory.class, classLoader);
		for (LayoutFactory factory : factories) {
			customTypes.put(factory.getName(), factory.getLayout());
		}
	}

}

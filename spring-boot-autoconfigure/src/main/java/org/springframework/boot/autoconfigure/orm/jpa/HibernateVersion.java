/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.orm.jpa;

import org.springframework.util.ClassUtils;

/**
 * Supported Hibernate versions.
 *
 * @author Phillip Webb
 */
enum HibernateVersion {

	/**
	 * Version 4.
	 */
	V4,

	/**
	 * Version 5.
	 */
	V5;

	private static final String HIBERNATE_5_CLASS = "org.hibernate.boot.model."
			+ "naming.PhysicalNamingStrategy";

	private static HibernateVersion running;

	public static HibernateVersion getRunning() {
		if (running == null) {
			setRunning(ClassUtils.isPresent(HIBERNATE_5_CLASS, null) ? V5 : V4);
		}
		return running;
	}

	static void setRunning(HibernateVersion running) {
		HibernateVersion.running = running;
	}

}

/*
 * Copyright 2012-2013 the original author or authors.
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
package org.springframework.boot.loader;

/**
 * Singleton for {@link PropertiesLauncher} so that multiple invocations of main in
 * in the same JVM will return the same {@link PropertiesLauncher} allowing for 
 * possibility to issue more commands to application besides just 
 * starting it (i.e. exit)
 * 
 * @author Andrew Wynham (DeezCashews)
 *
 */
public class SharedPropertiesLauncher extends PropertiesLauncher {

	protected static SharedPropertiesLauncher INSTANCE;
	
	SharedPropertiesLauncher() {
	}
	
	public static final SharedPropertiesLauncher getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new SharedPropertiesLauncher();
		}
		return INSTANCE;
	}

	public static void main(String[] args) {
		getInstance().launch(args);
	}
}

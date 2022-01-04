/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.info;

/**
 * Information about the Java environment the application is running in.
 *
 * @author Jonatan Ivanov
 * @author Stephane Nicoll
 * @since 2.6.0
 */
public class JavaInfo {

	private final String version;

	private final JavaVendorInfo vendor;

	private final JavaRuntimeEnvironmentInfo runtime;

	private final JavaVirtualMachineInfo jvm;

	public JavaInfo() {
		this.version = System.getProperty("java.version");
		this.vendor = new JavaVendorInfo();
		this.runtime = new JavaRuntimeEnvironmentInfo();
		this.jvm = new JavaVirtualMachineInfo();
	}

	public String getVersion() {
		return this.version;
	}

	public JavaVendorInfo getVendor() {
		return this.vendor;
	}

	public JavaRuntimeEnvironmentInfo getRuntime() {
		return this.runtime;
	}

	public JavaVirtualMachineInfo getJvm() {
		return this.jvm;
	}

	/**
	 * Information about the Java Vendor of the Java Runtime the application is running
	 * in.
	 *
	 * @since 2.7.0
	 */
	public static class JavaVendorInfo {

		private final String name;

		private final String version;

		public JavaVendorInfo() {
			this.name = System.getProperty("java.vendor");
			this.version = System.getProperty("java.vendor.version");
		}

		public String getName() {
			return this.name;
		}

		public String getVersion() {
			return this.version;
		}

	}

	/**
	 * Information about the Java Runtime Environment the application is running in.
	 */
	public static class JavaRuntimeEnvironmentInfo {

		private final String name;

		private final String version;

		public JavaRuntimeEnvironmentInfo() {
			this.name = System.getProperty("java.runtime.name");
			this.version = System.getProperty("java.runtime.version");
		}

		public String getName() {
			return this.name;
		}

		public String getVersion() {
			return this.version;
		}

	}

	/**
	 * Information about the Java Virtual Machine the application is running in.
	 */
	public static class JavaVirtualMachineInfo {

		private final String name;

		private final String vendor;

		private final String version;

		public JavaVirtualMachineInfo() {
			this.name = System.getProperty("java.vm.name");
			this.vendor = System.getProperty("java.vm.vendor");
			this.version = System.getProperty("java.vm.version");
		}

		public String getName() {
			return this.name;
		}

		public String getVendor() {
			return this.vendor;
		}

		public String getVersion() {
			return this.version;
		}

	}

}

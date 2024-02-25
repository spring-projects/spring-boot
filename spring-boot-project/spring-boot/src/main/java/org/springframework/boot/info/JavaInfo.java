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

	/**
     * Constructs a new JavaInfo object.
     * 
     * This constructor initializes the JavaInfo object with information about the Java version, vendor, runtime environment, and virtual machine.
     * The version is obtained using the System.getProperty("java.version") method.
     * The vendor information is obtained by creating a new JavaVendorInfo object.
     * The runtime environment information is obtained by creating a new JavaRuntimeEnvironmentInfo object.
     * The virtual machine information is obtained by creating a new JavaVirtualMachineInfo object.
     */
    public JavaInfo() {
		this.version = System.getProperty("java.version");
		this.vendor = new JavaVendorInfo();
		this.runtime = new JavaRuntimeEnvironmentInfo();
		this.jvm = new JavaVirtualMachineInfo();
	}

	/**
     * Returns the version of the JavaInfo.
     *
     * @return the version of the JavaInfo
     */
    public String getVersion() {
		return this.version;
	}

	/**
     * Returns the vendor information of the Java environment.
     *
     * @return the vendor information of the Java environment
     */
    public JavaVendorInfo getVendor() {
		return this.vendor;
	}

	/**
     * Returns the Java Runtime Environment information.
     *
     * @return the Java Runtime Environment information
     */
    public JavaRuntimeEnvironmentInfo getRuntime() {
		return this.runtime;
	}

	/**
     * Returns the information about the Java Virtual Machine (JVM).
     *
     * @return the JavaVirtualMachineInfo object representing the JVM information
     */
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

		/**
         * Constructs a new JavaVendorInfo object.
         * Retrieves the name and version of the Java vendor from the system properties.
         * 
         * @param name the name of the Java vendor
         * @param version the version of the Java vendor
         */
        public JavaVendorInfo() {
			this.name = System.getProperty("java.vendor");
			this.version = System.getProperty("java.vendor.version");
		}

		/**
         * Returns the name of the Java vendor.
         *
         * @return the name of the Java vendor
         */
        public String getName() {
			return this.name;
		}

		/**
         * Returns the version of the Java vendor.
         *
         * @return the version of the Java vendor
         */
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

		/**
         * Constructs a new JavaRuntimeEnvironmentInfo object.
         * Retrieves the name and version of the Java Runtime Environment
         * using the System.getProperty() method.
         * 
         * @param name the name of the Java Runtime Environment
         * @param version the version of the Java Runtime Environment
         */
        public JavaRuntimeEnvironmentInfo() {
			this.name = System.getProperty("java.runtime.name");
			this.version = System.getProperty("java.runtime.version");
		}

		/**
         * Returns the name of the Java Runtime Environment.
         *
         * @return the name of the Java Runtime Environment
         */
        public String getName() {
			return this.name;
		}

		/**
         * Returns the version of the Java Runtime Environment.
         *
         * @return the version of the Java Runtime Environment
         */
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

		/**
         * Constructs a new JavaVirtualMachineInfo object.
         * Retrieves the name, vendor, and version information of the Java Virtual Machine (JVM) 
         * using the System.getProperty() method.
         * 
         * @param name the name of the JVM
         * @param vendor the vendor of the JVM
         * @param version the version of the JVM
         */
        public JavaVirtualMachineInfo() {
			this.name = System.getProperty("java.vm.name");
			this.vendor = System.getProperty("java.vm.vendor");
			this.version = System.getProperty("java.vm.version");
		}

		/**
         * Returns the name of the Java Virtual Machine.
         *
         * @return the name of the Java Virtual Machine
         */
        public String getName() {
			return this.name;
		}

		/**
         * Returns the vendor of the Java Virtual Machine.
         *
         * @return the vendor of the Java Virtual Machine
         */
        public String getVendor() {
			return this.vendor;
		}

		/**
         * Returns the version of the Java Virtual Machine.
         *
         * @return the version of the Java Virtual Machine
         */
        public String getVersion() {
			return this.version;
		}

	}

}

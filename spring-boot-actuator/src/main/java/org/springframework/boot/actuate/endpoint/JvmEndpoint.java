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

package org.springframework.boot.actuate.endpoint;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.boot.ApplicationPid;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link Endpoint} to expose JVM information. When the application is running on HotSpot JDK
 * it prints flag values. In other case prints just application input parameters.
 *
 * @author Jakub Kubrynski
 */
public class JvmEndpoint extends AbstractEndpoint<Map<String, String>> implements BeanClassLoaderAware {

	/**
	 * VirtualMachine instance. Should be static due to creating tools.jar classLoader
	 * which can be created only once per JVM.
	 */
	private static Object vmInstance;

	static final String NON_DEFAULT_FLAGS = "non-default_flags";
	static final String DEMANDED_FLAGS = "demanded_flags";
	static final String NOT_FOUND_MESSAGE = "<cannot retrieve>";

	private static final String[] TOOLS_LOCATIONS = {"lib/tools.jar", "../lib/tools.jar", "../Classes/classes.jar"};
	private static final String VIRTUAL_MACHINE_CLASS = "com.sun.tools.attach.VirtualMachine";

	private final List<String> flagsToInclude;
	private ClassLoader beanClassLoader;

	/**
	 * Create a new {@link JvmEndpoint} instance.
	 */
	public JvmEndpoint() {
		this(Collections.<String>emptyList());
	}

	/**
	 * Create a new {@link JvmEndpoint} instance.
	 *
	 * @param flagsToInclude list of specific JVM flags to include in output.
	 *                       used in the same way as in jinfo -flag FLAG_NAME
	 */
	public JvmEndpoint(List<String> flagsToInclude) {
		super("jvm", true);
		this.flagsToInclude = flagsToInclude;
	}

	@Override
	public Map<String, String> invoke() {
		tryAttachToJvm();

		HashMap<String, String> result = new HashMap<String, String>();
		if (vmInstance != null) {
			result.put(NON_DEFAULT_FLAGS, invokeVmMethod("executeJCmd", "VM.flags"));
			if (!flagsToInclude.isEmpty()) {
				result.put(DEMANDED_FLAGS, getDemandedFlagsValues());
			}
		} else {
			result.put(NON_DEFAULT_FLAGS, getInputArguments());
		}
		return result;
	}

	private String getInputArguments() {
		return StringUtils.collectionToDelimitedString(ManagementFactory.getRuntimeMXBean().getInputArguments(), " ");
	}

	private String getDemandedFlagsValues() {
		List<String> values = new ArrayList<String>();
		for (String flagName : flagsToInclude) {
			String flagValue = invokeVmMethod("printFlag", flagName);
			if (StringUtils.hasText(flagValue)) {
				values.add(flagValue.replaceAll("\n", ""));
			}
		}
		return StringUtils.collectionToDelimitedString(values, " ");
	}


	private String invokeVmMethod(String methodName, String parameter) {
		try {
			Method executeJCmd = vmInstance.getClass().getMethod(methodName, String.class);
			InputStream invoke = (InputStream) executeJCmd.invoke(vmInstance, parameter);
			return IOUtils.toString(invoke);
		} catch (Exception ignore) {
			return NOT_FOUND_MESSAGE;
		}
	}

	/**
	 * Lazy attach to JVM instance
	 */
	private void tryAttachToJvm() {
		if (vmInstance == null) {
			try {
				Class<?> vmClass;
				try {
					vmClass = beanClassLoader.loadClass(VIRTUAL_MACHINE_CLASS);
				} catch (ClassNotFoundException e) {
					ClassLoader toolsClassLoader = new URLClassLoader(new URL[]{getToolsJarUrl()});
					vmClass = toolsClassLoader.loadClass(VIRTUAL_MACHINE_CLASS);
				}
				Method attachMethod = vmClass.getDeclaredMethod("attach", String.class);
				String pid = new ApplicationPid().getPid();
				vmInstance = attachMethod.invoke(null, pid);
			} catch (Exception ignore) {
			}
		}
	}

	private URL getToolsJarUrl() throws MalformedURLException {
		for (String location : TOOLS_LOCATIONS) {
			File url = new File(System.getProperty("java.home") + File.separator + location);
			if (url.exists()) {
				return url.toURI().toURL();
			}
		}
		throw new IllegalStateException("Unable to locate tools.jar");
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		beanClassLoader = classLoader;
	}
}

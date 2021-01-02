/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.loader;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.ExplodedArchive;
import org.springframework.boot.loader.archive.JarFileArchive;
import org.springframework.boot.loader.jar.JarFile;

/** 启动器的父类，可以用一个或多个 {@link Archive} 支持的完全配置的类路径启动应用程序
 * <p> Base class for launchers that can start an application with a fully configured
 * classpath backed by one or more {@link Archive}s.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @since 1.0.0
 */
public abstract class Launcher {

	/** 启动应用程序
	 * <p> Launch the application. This method is the initial entry point that should be
	 * called by a subclass {@code public static void main(String[] args)} method.
	 * @param args the incoming arguments
	 * @throws Exception if the application fails to launch
	 */
	protected void launch(String[] args) throws Exception {
		// 注册 URL 协议的处理器
		JarFile.registerUrlProtocolHandler();
		/**
		 * {@link LaunchedURLClassLoader}，创建类加载器
		 */
		ClassLoader classLoader = createClassLoader(getClassPathArchives());
		// 执行启动类的 main 方法
		launch(args, getMainClass(), classLoader);
	}

	/** 创建自定义的 {@link ClassLoader} 实现类，用于从 jar 包中加载类。
	 * 基于获得的 {@link Archive} 数组，创建自定义 {@link ClassLoader} 实现类
	 * {@link LaunchedURLClassLoader}，通过它来加载 {@literal 'BOOT-INF/classes'}
	 * 目录下的类，以及 {@literal 'BOOT-INF/lib'} 目录下的 jar 包中的类
	 * <p> Create a classloader for the specified archives.
	 * @param archives the archives
	 * @return the classloader
	 * @throws Exception if the classloader cannot be created
	 */
	protected ClassLoader createClassLoader(List<Archive> archives) throws Exception {
		// 获得所有 Archive 的 URL 地址
		List<URL> urls = new ArrayList<>(archives.size());
		for (Archive archive : archives) {
			urls.add(archive.getUrl());
		}
		// 创建加载这些 URL 的 ClassLoader
		return createClassLoader(urls.toArray(new URL[0]));
	}

	/**
	 * Create a classloader for the specified URLs.
	 * @param urls the URLs
	 * @return the classloader
	 * @throws Exception if the classloader cannot be created
	 */
	protected ClassLoader createClassLoader(URL[] urls) throws Exception {
		return new LaunchedURLClassLoader(urls, getClass().getClassLoader());
	}

	/** 执行我们声明的 Spring Boot 启动类，进行 Spring Boot 应用的启动
	 * <p> Launch the application given the archive file and a fully configured classloader.
	 * @param args the incoming arguments
	 * @param mainClass the main class to run
	 * @param classLoader the classloader
	 * @throws Exception if the launch fails
	 */
	protected void launch(String[] args, String mainClass, ClassLoader classLoader) throws Exception {
		/**
		 * 设置 {@link LaunchedURLClassLoader} 作为类加载器
		 * 从而保证能够从 jar 加载到相应的类
		 */
		Thread.currentThread().setContextClassLoader(classLoader);
		// 创建 MainMethodRunner 对象，并执行 run 方法，启动 Spring Boot 应用
		/**
		 * 创建 {@link MainMethodRunner} 对象，并执行 {@link MainMethodRunner#run()}
		 * 方法来启动 Spring Boot 应用。
		 */
		createMainMethodRunner(mainClass, args, classLoader).run();
	}

	/**
	 * Create the {@code MainMethodRunner} used to launch the application.
	 * @param mainClass the main class
	 * @param args the incoming arguments
	 * @param classLoader the classloader
	 * @return the main method runner
	 */
	protected MainMethodRunner createMainMethodRunner(String mainClass, String[] args, ClassLoader classLoader) {
		return new MainMethodRunner(mainClass, args);
	}

	/**
	 * Returns the main class that should be launched.
	 * @return the name of the main class
	 * @throws Exception if the main class cannot be obtained
	 */
	protected abstract String getMainClass() throws Exception;

	/**
	 * Returns the archives that will be used to construct the class path.
	 * @return the class path archives
	 * @throws Exception if the class path archives cannot be obtained
	 */
	protected abstract List<Archive> getClassPathArchives() throws Exception;

	protected final Archive createArchive() throws Exception {
		// 获得 jar 所在的绝对路径
		ProtectionDomain protectionDomain = getClass().getProtectionDomain();
		CodeSource codeSource = protectionDomain.getCodeSource();
		URI location = (codeSource != null) ? codeSource.getLocation().toURI() : null;
		String path = (location != null) ? location.getSchemeSpecificPart() : null;
		if (path == null) {
			throw new IllegalStateException("Unable to determine code source archive");
		}
		// jar 包的绝对地址
		File root = new File(path);
		if (!root.exists()) {
			throw new IllegalStateException("Unable to determine code source archive from " + root);
		}
		// 如果是目录，则使用 ExplodedArchive 进行展开
		// 如果不是目录，则使用 JarFileArchive
		return (root.isDirectory() ? new ExplodedArchive(root) : new JarFileArchive(root));
	}

}

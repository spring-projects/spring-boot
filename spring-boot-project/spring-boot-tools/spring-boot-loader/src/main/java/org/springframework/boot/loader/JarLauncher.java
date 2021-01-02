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

import org.springframework.boot.loader.archive.Archive;

/** 基于 jar 的启动器，这个启动器假设 jar 包含在 {@code /BOOT-INF/lib} 目录中，应用程序类包含在
 * {@code /BOOT-INF/classes} 目录中
 * <ul>
 *     <li>BOOT-INF/classes/ 目录被归类为一个 Archive 对象</li>
 *     <li>BOOT-INF/lib/ 目录下的每个内嵌 jar 包都对应一个 Archive 对象</li>
 * </ul>
 * <h3>jar 启动的原理</h3>
 * <ul>
 *     <li>通过 {@link Archive}，实现 jar 包的遍历，将 {@literal 'BOOT-INF/classes'} 目录和
 *     {@literal 'META-INF/lib'} 的每一个内嵌的 jar 解析成一个 Archive 对象。</li>
 *     <li>通过 {@link org.springframework.boot.loader.jar.Handler}，处理 jar: 协议的 URL
 *     的资源读取，也就是读取了每个 {@link Archive} 里的内容。</li>
 *     <li>通过 {@link LaunchedURLClassLoader}，实现 {@literal 'BOOT-INF/classes'} 目录下的类
 *     和 {@literal 'BOOT-INF/classes'} 目录下内嵌的 jar 包中的类的加载。具体的 URL 来源，
 *     是通过 {@link Archive} 提供；具体 URL 的读取，是通过 {@link org.springframework.boot.loader.jar.Handler}
 *     提供。</li>
 * </ul>
 * <p> {@link Launcher} for JAR based archives. This launcher assumes that dependency jars are
 * included inside a {@code /BOOT-INF/lib} directory and that application classes are
 * included inside a {@code /BOOT-INF/classes} directory.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.0.0
 */
public class JarLauncher extends ExecutableArchiveLauncher {

	/**
	 * 应用程序目录 BOOT-INF/classes/
	 */
	static final String BOOT_INF_CLASSES = "BOOT-INF/classes/";

	/**
	 * 内嵌的 jar 包目录 BOOT-INF/lib/
	 */
	static final String BOOT_INF_LIB = "BOOT-INF/lib/";

	public JarLauncher() {
	}

	protected JarLauncher(Archive archive) {
		super(archive);
	}

	/**
	 * 是否是内嵌的归档
	 * @param entry the jar entry
	 * @return true 是内嵌的归档
	 */
	@Override
	protected boolean isNestedArchive(Archive.Entry entry) {
		// 如果是目录的情况，只要 BOOT-INF/classes/ 目录
		if (entry.isDirectory()) {
			return entry.getName().equals(BOOT_INF_CLASSES);
		}
		// 如果是文件的情况，只要 BOOT-INF/lib/ 目录下的 `jar` 包
		return entry.getName().startsWith(BOOT_INF_LIB);
	}

	/**
	 * {@link JarLauncher} 启动入口
	 * <p> 在打出的 jar 包中的 META-INF/MANIFEST.MF 中
	 * Main-Class: org.springframework.boot.loader.JarLauncher
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		new JarLauncher().launch(args);
	}

}

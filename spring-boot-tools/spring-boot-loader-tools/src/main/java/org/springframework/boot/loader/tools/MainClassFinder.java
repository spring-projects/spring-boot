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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.Type;

/**
 * Finds any class with a {@code public static main} method by performing a breadth first
 * search.
 *
 * @author Phillip Webb
 */
public abstract class MainClassFinder {

	private static final String DOT_CLASS = ".class";

	private static final Type STRING_ARRAY_TYPE = Type.getType(String[].class);

	private static final Type MAIN_METHOD_TYPE = Type.getMethodType(Type.VOID_TYPE,
			STRING_ARRAY_TYPE);

	private static final String MAIN_METHOD_NAME = "main";

	private static final FileFilter CLASS_FILE_FILTER = new FileFilter() {
		@Override
		public boolean accept(File file) {
			return (file.isFile() && file.getName().endsWith(DOT_CLASS));
		}
	};

	private static final FileFilter PACKAGE_FOLDER_FILTER = new FileFilter() {
		@Override
		public boolean accept(File file) {
			return file.isDirectory() && !file.getName().startsWith(".");
		}
	};

	/**
	 * Find the main class from a given folder.
	 * @param rootFolder the root folder to search
	 * @return the main class or {@code null}
	 * @throws IOException if the folder cannot be read
	 */
	public static String findMainClass(File rootFolder) throws IOException {
		return doWithMainClasses(rootFolder, new ClassNameCallback<String>() {
			@Override
			public String doWith(String className) {
				return className;
			}
		});
	}

	/**
	 * Find a single main class from a given folder.
	 * @param rootFolder the root folder to search
	 * @return the main class or {@code null}
	 * @throws IOException if the folder cannot be read
	 */
	public static String findSingleMainClass(File rootFolder) throws IOException {
		MainClassesCallback callback = new MainClassesCallback();
		MainClassFinder.doWithMainClasses(rootFolder, callback);
		return callback.getMainClass();
	}

	/**
	 * Perform the given callback operation on all main classes from the given root
	 * folder.
	 * @param <T> the result type
	 * @param rootFolder the root folder
	 * @param callback the callback
	 * @return the first callback result or {@code null}
	 * @throws IOException in case of I/O errors
	 */
	static <T> T doWithMainClasses(File rootFolder, ClassNameCallback<T> callback)
			throws IOException {
		if (!rootFolder.exists()) {
			return null; // nothing to do
		}
		if (!rootFolder.isDirectory()) {
			throw new IllegalArgumentException(
					"Invalid root folder '" + rootFolder + "'");
		}
		String prefix = rootFolder.getAbsolutePath() + "/";
		Deque<File> stack = new ArrayDeque<File>();
		stack.push(rootFolder);
		while (!stack.isEmpty()) {
			File file = stack.pop();
			if (file.isFile()) {
				InputStream inputStream = new FileInputStream(file);
				try {
					if (isMainClass(inputStream)) {
						String className = convertToClassName(file.getAbsolutePath(),
								prefix);
						T result = callback.doWith(className);
						if (result != null) {
							return result;
						}
					}
				}
				finally {
					inputStream.close();
				}
			}
			if (file.isDirectory()) {
				pushAllSorted(stack, file.listFiles(PACKAGE_FOLDER_FILTER));
				pushAllSorted(stack, file.listFiles(CLASS_FILE_FILTER));
			}
		}
		return null;
	}

	private static void pushAllSorted(Deque<File> stack, File[] files) {
		Arrays.sort(files, new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		for (File file : files) {
			stack.push(file);
		}
	}

	/**
	 * Find the main class in a given jar file.
	 * @param jarFile the jar file to search
	 * @param classesLocation the location within the jar containing classes
	 * @return the main class or {@code null}
	 * @throws IOException if the jar file cannot be read
	 */
	public static String findMainClass(JarFile jarFile, String classesLocation)
			throws IOException {
		return doWithMainClasses(jarFile, classesLocation,
				new ClassNameCallback<String>() {
					@Override
					public String doWith(String className) {
						return className;
					}
				});
	}

	/**
	 * Find a single main class in a given jar file.
	 * @param jarFile the jar file to search
	 * @param classesLocation the location within the jar containing classes
	 * @return the main class or {@code null}
	 * @throws IOException if the jar file cannot be read
	 */
	public static String findSingleMainClass(JarFile jarFile, String classesLocation)
			throws IOException {
		MainClassesCallback callback = new MainClassesCallback();
		MainClassFinder.doWithMainClasses(jarFile, classesLocation, callback);
		return callback.getMainClass();
	}

	/**
	 * Perform the given callback operation on all main classes from the given jar.
	 * @param <T> the result type
	 * @param jarFile the jar file to search
	 * @param classesLocation the location within the jar containing classes
	 * @param callback the callback
	 * @return the first callback result or {@code null}
	 * @throws IOException in case of I/O errors
	 */
	static <T> T doWithMainClasses(JarFile jarFile, String classesLocation,
			ClassNameCallback<T> callback) throws IOException {
		List<JarEntry> classEntries = getClassEntries(jarFile, classesLocation);
		Collections.sort(classEntries, new ClassEntryComparator());
		for (JarEntry entry : classEntries) {
			InputStream inputStream = new BufferedInputStream(
					jarFile.getInputStream(entry));
			try {
				if (isMainClass(inputStream)) {
					String className = convertToClassName(entry.getName(),
							classesLocation);
					T result = callback.doWith(className);
					if (result != null) {
						return result;
					}
				}
			}
			finally {
				inputStream.close();
			}
		}
		return null;
	}

	private static String convertToClassName(String name, String prefix) {
		name = name.replace("/", ".");
		name = name.replace('\\', '.');
		name = name.substring(0, name.length() - DOT_CLASS.length());
		if (prefix != null) {
			name = name.substring(prefix.length());
		}
		return name;
	}

	private static List<JarEntry> getClassEntries(JarFile source,
			String classesLocation) {
		classesLocation = (classesLocation != null ? classesLocation : "");
		Enumeration<JarEntry> sourceEntries = source.entries();
		List<JarEntry> classEntries = new ArrayList<JarEntry>();
		while (sourceEntries.hasMoreElements()) {
			JarEntry entry = sourceEntries.nextElement();
			if (entry.getName().startsWith(classesLocation)
					&& entry.getName().endsWith(DOT_CLASS)) {
				classEntries.add(entry);
			}
		}
		return classEntries;
	}

	private static boolean isMainClass(InputStream inputStream) {
		try {
			ClassReader classReader = new ClassReader(inputStream);
			MainMethodFinder mainMethodFinder = new MainMethodFinder();
			classReader.accept(mainMethodFinder, ClassReader.SKIP_CODE);
			return mainMethodFinder.isFound();
		}
		catch (IOException ex) {
			return false;
		}
	}

	private static class ClassEntryComparator implements Comparator<JarEntry> {

		@Override
		public int compare(JarEntry o1, JarEntry o2) {
			Integer d1 = getDepth(o1);
			Integer d2 = getDepth(o2);
			int depthCompare = d1.compareTo(d2);
			if (depthCompare != 0) {
				return depthCompare;
			}
			return o1.getName().compareTo(o2.getName());
		}

		private int getDepth(JarEntry entry) {
			return entry.getName().split("/").length;
		}

	}

	private static class MainMethodFinder extends ClassVisitor {

		private boolean found;

		MainMethodFinder() {
			super(Opcodes.ASM4);
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String desc,
				String signature, String[] exceptions) {
			if (isAccess(access, Opcodes.ACC_PUBLIC, Opcodes.ACC_STATIC)
					&& MAIN_METHOD_NAME.equals(name)
					&& MAIN_METHOD_TYPE.getDescriptor().equals(desc)) {
				this.found = true;
			}
			return null;
		}

		private boolean isAccess(int access, int... requiredOpsCodes) {
			for (int requiredOpsCode : requiredOpsCodes) {
				if ((access & requiredOpsCode) == 0) {
					return false;
				}
			}
			return true;
		}

		public boolean isFound() {
			return this.found;
		}

	}

	/**
	 * Callback interface used to receive class names.
	 * @param <T> the result type
	 */
	public interface ClassNameCallback<T> {

		/**
		 * Handle the specified class name.
		 * @param className the class name
		 * @return a non-null value if processing should end or {@code null} to continue
		 */
		T doWith(String className);

	}

	/**
	 * Find a single main class, throwing an {@link IllegalStateException} if multiple
	 * candidates exist.
	 */
	private static class MainClassesCallback implements ClassNameCallback<Object> {

		private final Set<String> classNames = new LinkedHashSet<String>();

		@Override
		public Object doWith(String className) {
			this.classNames.add(className);
			return null;
		}

		public String getMainClass() {
			if (this.classNames.size() > 1) {
				throw new IllegalStateException(
						"Unable to find a single main class from the following candidates "
								+ this.classNames);
			}
			return this.classNames.isEmpty() ? null : this.classNames.iterator().next();
		}

	}

}

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

package org.springframework.zero.maven;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Finds any class with a {@code public static main} method by performing a breadth first
 * directory search.
 * 
 * @author Phillip Webb
 */
abstract class MainClassFinder {

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

	public static String findMainClass(File root) {
		File mainClassFile = findMainClassFile(root);
		if (mainClassFile == null) {
			return null;
		}
		String mainClass = mainClassFile.getAbsolutePath().substring(
				root.getAbsolutePath().length() + 1);
		mainClass = mainClass.replace('/', '.');
		mainClass = mainClass.replace('\\', '.');
		mainClass = mainClass.substring(0, mainClass.length() - DOT_CLASS.length());
		return mainClass;
	}

	public static File findMainClassFile(File root) {
		Deque<File> stack = new ArrayDeque<File>();
		stack.push(root);
		while (!stack.isEmpty()) {
			File file = stack.pop();
			if (isMainClassFile(file)) {
				return file;
			}
			if (file.isDirectory()) {
				pushAllSorted(stack, file.listFiles(PACKAGE_FOLDER_FILTER));
				pushAllSorted(stack, file.listFiles(CLASS_FILE_FILTER));
			}
		}
		return null;
	}

	private static boolean isMainClassFile(File file) {
		try {
			InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
			try {
				ClassReader classReader = new ClassReader(inputStream);
				MainMethodFinder mainMethodFinder = new MainMethodFinder();
				classReader.accept(mainMethodFinder, ClassReader.SKIP_CODE);
				return mainMethodFinder.isFound();
			}
			finally {
				inputStream.close();
			}
		}
		catch (IOException ex) {
			return false;
		}
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

	private static class MainMethodFinder extends ClassVisitor {

		private boolean found;

		public MainMethodFinder() {
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
}

/*
 * Copyright 2025-present the original author or authors.
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

package org.springframework.boot.build.autoconfigure;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.SpringAsmInfo;
import org.springframework.asm.Type;

/**
 * An {@code @AutoConfiguration} class.
 *
 * @param name name of the auto-configuration class
 * @param before values of the {@code before} attribute
 * @param beforeName values of the {@code beforeName} attribute
 * @param after values of the {@code after} attribute
 * @param afterName values of the {@code afterName} attribute
 * @author Andy Wilkinson
 */
public record AutoConfigurationClass(String name, List<String> before, List<String> beforeName, List<String> after,
		List<String> afterName) {

	private AutoConfigurationClass(String name, Map<String, List<String>> attributes) {
		this(name, attributes.getOrDefault("before", Collections.emptyList()),
				attributes.getOrDefault("beforeName", Collections.emptyList()),
				attributes.getOrDefault("after", Collections.emptyList()),
				attributes.getOrDefault("afterName", Collections.emptyList()));
	}

	static AutoConfigurationClass of(File classFile) {
		try (FileInputStream input = new FileInputStream(classFile)) {
			ClassReader classReader = new ClassReader(input);
			AutoConfigurationClassVisitor visitor = new AutoConfigurationClassVisitor();
			classReader.accept(visitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
			return visitor.autoConfigurationClass;
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private static final class AutoConfigurationClassVisitor extends ClassVisitor {

		private AutoConfigurationClass autoConfigurationClass;

		private String name;

		private AutoConfigurationClassVisitor() {
			super(SpringAsmInfo.ASM_VERSION);
		}

		@Override
		public void visit(int version, int access, String name, String signature, String superName,
				String[] interfaces) {
			this.name = Type.getObjectType(name).getClassName();
		}

		@Override
		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			String annotationClassName = Type.getType(descriptor).getClassName();
			if ("org.springframework.boot.autoconfigure.AutoConfiguration".equals(annotationClassName)) {
				return new AutoConfigurationAnnotationVisitor();
			}
			return null;
		}

		private final class AutoConfigurationAnnotationVisitor extends AnnotationVisitor {

			private Map<String, List<String>> attributes = new HashMap<>();

			private static final Set<String> INTERESTING_ATTRIBUTES = Set.of("before", "beforeName", "after",
					"afterName");

			private AutoConfigurationAnnotationVisitor() {
				super(SpringAsmInfo.ASM_VERSION);
			}

			@Override
			public void visitEnd() {
				AutoConfigurationClassVisitor.this.autoConfigurationClass = new AutoConfigurationClass(
						AutoConfigurationClassVisitor.this.name, this.attributes);
			}

			@Override
			public AnnotationVisitor visitArray(String attributeName) {
				if (INTERESTING_ATTRIBUTES.contains(attributeName)) {
					return new AnnotationVisitor(SpringAsmInfo.ASM_VERSION) {

						@Override
						public void visit(String name, Object value) {
							if (value instanceof Type type) {
								value = type.getClassName();
							}
							AutoConfigurationAnnotationVisitor.this.attributes
								.computeIfAbsent(attributeName, (n) -> new ArrayList<>())
								.add(Objects.toString(value));
						}

					};
				}
				return null;
			}

		}

	}

}

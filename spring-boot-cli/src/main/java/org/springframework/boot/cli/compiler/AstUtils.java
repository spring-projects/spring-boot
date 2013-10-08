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

package org.springframework.boot.cli.compiler;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;

/**
 * General purpose AST utilities.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 * @author Greg Turnquist
 */
public abstract class AstUtils {

	/**
	 * Determine if an {@link AnnotatedNode} has one or more of the specified annotations.
	 * N.B. the annotation type names are not normally fully qualified.
	 */
	public static boolean hasAtLeastOneAnnotation(AnnotatedNode node,
			String... annotations) {

		for (AnnotationNode annotationNode : node.getAnnotations()) {
			for (String annotation : annotations) {
				if (annotation.equals(annotationNode.getClassNode().getName())) {
					return true;
				}
			}
		}

		return false;

	}

    /**
     * Determine if a {@link ClassNode} has one or more of the specified annotations on the class
     * or any of its methods.
     * N.B. the type names are not normally fully qualified.
     */
    public static boolean hasAtLeastOneAnnotation(ClassNode node, String... annotations) {
        for (AnnotationNode annotationNode : node.getAnnotations()) {
            for (String annotation : annotations) {
                if (annotation.equals(annotationNode.getClassNode().getName())) {
                    return true;
                }
            }
        }

        List<MethodNode> methods = node.getMethods();
        for (MethodNode method : methods) {
            for (AnnotationNode annotationNode : method.getAnnotations()) {
                for (String annotation : annotations) {
                    if (annotation.equals(annotationNode.getClassNode().getName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

	/**
	 * Determine if a {@link ClassNode} has one or more fields of the specified types or
	 * method returning one or more of the specified types. N.B. the type names are not
	 * normally fully qualified.
	 */
	public static boolean hasAtLeastOneFieldOrMethod(ClassNode node, String... types) {

		Set<String> set = new HashSet<String>(Arrays.asList(types));
		List<FieldNode> fields = node.getFields();
		for (FieldNode field : fields) {
			if (set.contains(field.getType().getName())) {
				return true;
			}
		}
		List<MethodNode> methods = node.getMethods();
		for (MethodNode method : methods) {
			if (set.contains(method.getReturnType().getName())) {
				return true;
			}
		}

		return false;

	}

    /**
     * Determine if a {@link ClassNode} subclasses any of the specified types
     * N.B. the type names are not normally fully qualified.
     */
    public static boolean subclasses(ClassNode node, String... types) {
        for (String type : types) {
            if (node.getSuperClass().getName().equals(type)) {
                return true;
            }
        }

        return false;
    }

}

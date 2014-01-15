/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.cli.compiler.grape;

import java.util.List;

import org.eclipse.aether.graph.Dependency;

/**
 * An abstraction for accessing the managed dependencies that should be used to influence
 * the outcome of dependency resolution performed by Aether.
 * 
 * @author Andy Wilkinson
 */
public interface ManagedDependenciesFactory {

	List<Dependency> getManagedDependencies();
}

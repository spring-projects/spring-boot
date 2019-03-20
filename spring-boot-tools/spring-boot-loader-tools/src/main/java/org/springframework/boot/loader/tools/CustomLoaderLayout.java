/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.loader.tools;

import java.io.IOException;

/**
 * Additional interface that can be implemented by {@link Layout Layouts} that write their
 * own loader classes.
 *
 * @author Phillip Webb
 * @since 1.5.0
 */
public interface CustomLoaderLayout {

	/**
	 * Write the required loader classes into the JAR.
	 * @param writer the writer used to write the classes
	 * @throws IOException if the classes cannot be written
	 */
	void writeLoadedClasses(LoaderClassesWriter writer) throws IOException;

}

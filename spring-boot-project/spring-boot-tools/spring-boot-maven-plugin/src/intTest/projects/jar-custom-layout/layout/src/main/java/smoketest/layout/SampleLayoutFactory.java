/*
 * Copyright 2012-2020 the original author or authors.
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

package smoketest.layout;

import java.io.File;

import org.springframework.boot.loader.tools.Layout;
import org.springframework.boot.loader.tools.LayoutFactory;

/**
 * SampleLayoutFactory class.
 */
public class SampleLayoutFactory implements LayoutFactory {

	private String name = "sample";

	/**
     * Constructs a new SampleLayoutFactory.
     */
    public SampleLayoutFactory() {
	}

	/**
     * Constructs a new SampleLayoutFactory with the specified name.
     * 
     * @param name the name of the SampleLayoutFactory
     */
    public SampleLayoutFactory(String name) {
		this.name = name;
	}

	/**
     * Sets the name of the SampleLayoutFactory.
     * 
     * @param name the name to be set for the SampleLayoutFactory
     */
    public void setName(String name) {
		this.name = name;
	}

	/**
     * Returns the name of the SampleLayoutFactory.
     *
     * @return the name of the SampleLayoutFactory
     */
    public String getName() {
		return this.name;
	}

	/**
     * Returns the layout for the given source file.
     * 
     * @param source the source file for which the layout is to be obtained
     * @return the layout object representing the layout for the source file
     */
    @Override
	public Layout getLayout(File source) {
		return new SampleLayout(this.name);
	}

}

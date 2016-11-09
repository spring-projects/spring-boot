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

/**
 * Default implementation of layout factory that looks up the enum value and maps it to a
 * layout from {@link Layouts}.
 *
 * @author Dave Syer
 *
 */
public class DefaultLayoutFactory implements LayoutFactory {

	@Override
	public Layout getLayout(LayoutType type) {
		switch (type) {
		case JAR:
			return new Layouts.Jar();
		case WAR:
			return new Layouts.War();
		case ZIP:
			return new Layouts.Expanded();
		case MODULE:
			return new Layouts.Module();
		default:
			return new Layouts.None();
		}
	}

}

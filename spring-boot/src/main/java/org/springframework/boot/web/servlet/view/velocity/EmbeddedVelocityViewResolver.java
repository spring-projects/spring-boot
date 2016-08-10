/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.web.servlet.view.velocity;

import org.springframework.web.servlet.view.velocity.VelocityView;
import org.springframework.web.servlet.view.velocity.VelocityViewResolver;

/**
 * Extended version of {@link VelocityViewResolver} that uses
 * {@link EmbeddedVelocityToolboxView} when the {@link #setToolboxConfigLocation(String)
 * toolboxConfigLocation} is set.
 *
 * @author Phillip Webb
 * @since 1.2.5
 * @deprecated as of 1.4 following the deprecation of Velocity support in Spring Framework
 * 4.3
 */
@Deprecated
public class EmbeddedVelocityViewResolver extends VelocityViewResolver {

	private String toolboxConfigLocation;

	@Override
	protected void initApplicationContext() {
		if (this.toolboxConfigLocation != null) {
			if (VelocityView.class.equals(getViewClass())) {
				this.logger.info("Using EmbeddedVelocityToolboxView instead of "
						+ "default VelocityView due to specified toolboxConfigLocation");
				setViewClass(EmbeddedVelocityToolboxView.class);
			}
		}
		super.initApplicationContext();
	}

	@Override
	public void setToolboxConfigLocation(String toolboxConfigLocation) {
		super.setToolboxConfigLocation(toolboxConfigLocation);
		this.toolboxConfigLocation = toolboxConfigLocation;
	}

}

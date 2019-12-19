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

package org.springframework.boot.autoconfigure;

/**
 * {@link org.springframework.context.annotation.DeferredImportSelector} that is used by
 * {@link ImportBeforeAutoConfiguration}.
 * <p>
 * This deferred import selector makes sure specified configurations to run BEFORE auto
 * configurations.
 *
 * @author Tadaya Tsuyukubo
 * @since 2.3.0
 */
public class ImportBeforeAutoConfigurationDeferredImportSelector
		extends AbstractImportBeforeAndAfterAutoConfigurationDeferredImportSelector {

	@Override
	public int getOrder() {
		return ORDER_BEFORE_AUTO_CONFIGURATION;
	}

	@Override
	protected Class<?> getAnnotationClass() {
		return ImportBeforeAutoConfiguration.class;
	}

	@Override
	public Class<? extends Group> getImportGroup() {
		return BeforeAutoConfigurationGroup.class;
	}

	/**
	 * {@link org.springframework.context.annotation.DeferredImportSelector.Group} for
	 * {@link ImportBeforeAutoConfigurationDeferredImportSelector}.
	 */
	private static class BeforeAutoConfigurationGroup extends AbstractBeforeAndAfterAutoConfigurationGroup {

	}

}

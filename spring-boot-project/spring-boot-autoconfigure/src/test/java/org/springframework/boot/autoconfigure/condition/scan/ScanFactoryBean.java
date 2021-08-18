/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.condition.scan;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.Assert;

class ScanFactoryBean implements FactoryBean<ScanBean> {

	ScanFactoryBean(String value) {
		Assert.state(!value.contains("$"), "value should not contain '$'");
	}

	@Override
	public ScanBean getObject() {
		return new ScanBean("fromFactory");
	}

	@Override
	public Class<?> getObjectType() {
		return ScanBean.class;
	}

	@Override
	public boolean isSingleton() {
		return false;
	}

}

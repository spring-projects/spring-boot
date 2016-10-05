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

package org.springframework.boot.actuate.metrics.dropwizard;

import com.codahale.metrics.Reservoir;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;

/**
 * A {@link Reservoir} factory to instantiate the Reservoir that will be set as default
 * for the {@link DropwizardMetricServices}.
 * The Reservoir instances can't be shared across {@link com.codahale.metrics.Metric}.
 *
 * @author Lucas Saldanha
 */
public abstract class ReservoirFactory implements ObjectFactory<Reservoir> {

	protected abstract Reservoir defaultReservoir();

	@Override
	public Reservoir getObject() throws BeansException {
		return defaultReservoir();
	}
}

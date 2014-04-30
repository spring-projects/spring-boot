/*
 * Copyright 2010-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample.data.gemfire.service;

import org.springframework.data.gemfire.repository.GemfireRepository;

import sample.data.gemfire.domain.Gemstone;

/**
 * The GemstoneRepository interface is an extension of the GemfireRepository abstraction
 * for encapsulating data access and persistence operations (CRUD) on Gemstone domain
 * objects.
 * 
 * @author John Blum
 */
public interface GemstoneRepository extends GemfireRepository<Gemstone, Long> {

	/**
	 * Finds a particular Gemstone in the GemFire Cache by name.
	 * @param <T> the Class type of the Gemstone domain object to find.
	 * @param name a String value indicating the name of the Gemstone to find.
	 * @return a Gemstone by name.
	 * @see sample.data.gemfire.domain.Gemstone
	 */
	<T extends Gemstone> T findByName(String name);

}

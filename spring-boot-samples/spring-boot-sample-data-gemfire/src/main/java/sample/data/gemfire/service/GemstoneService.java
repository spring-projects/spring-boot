/*
 * Copyright 2012-2017 the original author or authors.
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

package sample.data.gemfire.service;

import sample.data.gemfire.domain.Gemstone;

/**
 * The GemstoneService interface is a Service interface object contract defining business
 * operations for processing Gemstone domain objects.
 *
 * @author John Blum
 */
public interface GemstoneService {

	/**
	 * Returns a count of the number of Gemstones in the GemFire Cache.
	 * @return a long value indicating the number of Gemstones in the GemFire Cache.
	 */
	long count();

	/**
	 * Gets a Gemstone by ID.
	 * @param id a long value indicating the identifier of the Gemstone.
	 * @return a Gemstone with ID, or null if no Gemstone exists with ID.
	 * @see sample.data.gemfire.domain.Gemstone
	 */
	Gemstone get(Long id);

	/**
	 * Gets a Gemstone by name.
	 * @param name a String value indicating the name of the Gemstone.
	 * @return a Gemstone with name, or null if no Gemstone exists with name.
	 * @see sample.data.gemfire.domain.Gemstone
	 */
	Gemstone get(String name);

	/**
	 * Return a listing of Gemstones currently stored in the GemFire Cache.
	 * @return an Iterable object to iterate over the list of Gemstones currently stored
	 * in the GemFire Cache.
	 * @see java.lang.Iterable
	 * @see sample.data.gemfire.domain.Gemstone
	 */
	Iterable<Gemstone> list();

	/**
	 * Saves the specified Gemstone to the GemFire Cache.
	 * @param gemstone the Gemstone to save in the GemFire Cache.
	 * @return the saved Gemstone.
	 * @see sample.data.gemfire.domain.Gemstone
	 */
	Gemstone save(Gemstone gemstone);

}

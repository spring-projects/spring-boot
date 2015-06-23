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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import sample.data.gemfire.domain.Gemstone;

/**
 * The GemstoneServiceImpl class is a Service object implementing the GemstoneService
 * interface containing business logic and rules in addition to data services for
 * processing Gemstones.
 *
 * @author John Blum
 */
@Service("gemstoneService")
public class GemstoneServiceImpl implements GemstoneService {

	protected static final List<String> APPROVED_GEMS = new ArrayList<String>(
			Arrays.asList("ALEXANDRITE", "AQUAMARINE", "DIAMOND", "OPAL", "PEARL",
					"RUBY", "SAPPHIRE", "SPINEL", "TOPAZ"));

	@Autowired
	private GemstoneRepository gemstoneRepo;

	@PostConstruct
	public void init() {
		Assert.notNull(this.gemstoneRepo,
				"A reference to the 'GemstoneRepository' was not properly configured!");
		System.out.printf("%1$s initialized!%n", getClass().getSimpleName());
	}

	/**
	 * Returns a count of the number of Gemstones in the GemFire Cache.
	 * <p/>
	 *
	 * @return a long value indicating the number of Gemstones in the GemFire Cache.
	 */
	@Override
	@Transactional(readOnly = true)
	public long count() {
		return this.gemstoneRepo.count();
	}

	/**
	 * Gets a Gemstone by ID.
	 * <p/>
	 *
	 * @param id a long value indicating the identifier of the Gemstone.
	 * @return a Gemstone with ID, or null if no Gemstone exists with ID.
	 * @see sample.data.gemfire.domain.Gemstone
	 */
	@Override
	@Transactional(readOnly = true)
	public Gemstone get(final Long id) {
		return this.gemstoneRepo.findOne(id);
	}

	/**
	 * Gets a Gemstone by name.
	 * <p/>
	 *
	 * @param name a String value indicating the name of the Gemstone.
	 * @return a Gemstone with name, or null if no Gemstone exists with name.
	 * @see sample.data.gemfire.domain.Gemstone
	 */
	@Override
	@Transactional(readOnly = true)
	public Gemstone get(final String name) {
		return this.gemstoneRepo.findByName(name);
	}

	/**
	 * Return a listing of Gemstones currently stored in the GemFire Cache.
	 * <p/>
	 *
	 * @return a Iterable object to iterate over the list of Gemstones currently stored in
	 * the GemFire Cache.
	 * @see java.lang.Iterable
	 * @see sample.data.gemfire.domain.Gemstone
	 */
	@Override
	@Transactional(readOnly = true)
	public Iterable<Gemstone> list() {
		return this.gemstoneRepo.findAll();
	}

	/**
	 * Saves the specified Gemstone to the GemFire Cache.
	 * <p/>
	 *
	 * @param gemstone the Gemstone to save in the GemFire Cache.
	 * @return the saved Gemstone.
	 * @see sample.data.gemfire.domain.Gemstone
	 */
	@Override
	@Transactional
	public Gemstone save(final Gemstone gemstone) {
		Assert.notNull(gemstone, "The Gemstone to save must not be null!");
		Assert.notNull(gemstone.getName(), "The name of the Gemstone must be specified!");

		// NOTE deliberately (naively) validate the Gemstone after mutating data access in
		// GemFire rather than before
		// to demonstrate transactions in GemFire.
		Gemstone savedGemstone = validate(this.gemstoneRepo.save(gemstone));

		Assert.state(savedGemstone.equals(get(gemstone.getId())), String.format(
				"Failed to find Gemstone (%1$s) in GemFire's Cache Region 'Gemstones'!",
				gemstone));

		System.out.printf("Saved Gemstone (%1$s)%n", savedGemstone.getName());

		return gemstone;
	}

	private Gemstone validate(final Gemstone gemstone) {
		if (!APPROVED_GEMS.contains(gemstone.getName().toUpperCase())) {
			// NOTE if the Gemstone is not valid, blow chunks (should cause transaction to
			// rollback in GemFire)!
			System.err.printf("Illegal Gemstone (%1$s)!%n", gemstone.getName());
			throw new IllegalGemstoneException(String.format(
					"'%1$s' is not a valid Gemstone!", gemstone.getName()));
		}

		return gemstone;
	}

	public static final class IllegalGemstoneException extends IllegalArgumentException {

		public IllegalGemstoneException() {
		}

		public IllegalGemstoneException(final String message) {
			super(message);
		}

		public IllegalGemstoneException(final Throwable cause) {
			super(cause);
		}

		public IllegalGemstoneException(final String message, final Throwable cause) {
			super(message, cause);
		}

	}

}

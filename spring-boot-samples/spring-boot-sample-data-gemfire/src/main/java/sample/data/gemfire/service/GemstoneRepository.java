package sample.data.gemfire.service;

import org.springframework.data.gemfire.repository.GemfireRepository;

import sample.data.gemfire.domain.Gemstone;

/**
 * The GemstoneRepository interface is an extension of the GemfireRepository abstraction for encapsulating data access
 * and persistence operations (CRUD) on Gemstone domain objects.
 * <p/>
 * @author John Blum
 * @see org.springframework.data.gemfire.repository.GemfireRepository
 * @see sample.data.gemfire.domain.Gemstone
 * @since 1.0.0
 */
public interface GemstoneRepository extends GemfireRepository<Gemstone, Long> {

  /**
   * Finds a particular Gemstone in the GemFire Cache by name.
   * <p/>
   * @param <T> the Class type of the Gemstone domain object to find.
   * @param name a String value indicating the name of the Gemstone to find.
   * @return a Gemstone by name.
   * @see sample.data.gemfire.domain.Gemstone
   */
  <T extends Gemstone> T findByName(String name);

}

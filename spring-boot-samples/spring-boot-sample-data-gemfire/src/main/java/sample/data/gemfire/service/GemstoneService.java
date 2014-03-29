package sample.data.gemfire.service;

import sample.data.gemfire.domain.Gemstone;

/**
 * The GemstoneService interface is a Service interface object contract defining business operations for processing
 * Gemstone domain objects.
 * <p/>
 * @author John Blum
 * @see sample.data.gemfire.domain.Gemstone
 * @since 1.0.0
 */
@SuppressWarnings("unused")
public interface GemstoneService {

  /**
   * Returns a count of the number of Gemstones in the GemFire Cache.
   * <p/>
   * @return a long value indicating the number of Gemstones in the GemFire Cache.
   */
  long count();

  /**
   * Gets a Gemstone by ID.
   * <p/>
   * @param id a long value indicating the identifier of the Gemstone.
   * @return a Gemstone with ID, or null if no Gemstone exists with ID.
   * @see sample.data.gemfire.domain.Gemstone
   */
  Gemstone get(Long id);

  /**
   * Gets a Gemstone by name.
   * <p/>
   * @param name a String value indicating the name of the Gemstone.
   * @return a Gemstone with name, or null if no Gemstone exists with name.
   * @see sample.data.gemfire.domain.Gemstone
   */
  Gemstone get(String name);

  /**
   * Return a listing of Gemstones currently stored in the GemFire Cache.
   * <p/>
   * @return a Iterable object to iterate over the list of Gemstones currently stored in the GemFire Cache.
   * @see java.lang.Iterable
   * @see sample.data.gemfire.domain.Gemstone
   */
  Iterable<Gemstone> list();

  /**
   * Saves the specified Gemstone to the GemFire Cache.
   * <p/>
   * @param gemstone the Gemstone to save in the GemFire Cache.
   * @return the saved Gemstone.
   * @see sample.data.gemfire.domain.Gemstone
   */
  Gemstone save(Gemstone gemstone);

}

package sample.data.jpa.domain;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("spa")
public class SpaHotel extends Hotel {
  public SpaHotel(City city, String name) {
    super(city, name);
  }
}

package sample.data.jpa.domain;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

@Entity
@DiscriminatorValue("sport")
public class SportHotel extends Hotel {

  @ManyToOne
  private Sport mainSport;

}

package sample.data.jpa.domain;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("spa")
public class SpaHotel extends Hotel {
}

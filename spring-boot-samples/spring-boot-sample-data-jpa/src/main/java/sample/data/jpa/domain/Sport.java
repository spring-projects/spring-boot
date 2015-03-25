package sample.data.jpa.domain;

import static javax.persistence.EnumType.STRING;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class Sport {

  @Id
  @GeneratedValue
  private Long id;

  @Column
  @Enumerated(STRING)
  private SportType type;

  @Column(nullable = false)
  private String name;

  protected Sport() {
  }

  public Sport(String name) {
    this.name = name;
  }

  public SportType getType() {
    return type;
  }

  public String getName() {
    return name;
  }
}

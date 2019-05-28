package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.security.Label;

/** describes an abstract machine / principal with a given trust label. */
public class Host {
  private static final Host defaultHost = new Host("__DEFAULT__", Label.bottom());
  final String name;
  final Label label;

  public Host(String name) {
    this.name = name;
    this.label = Label.bottom();
  }

  public Host(String name, Label label) {
    this.name = name;
    this.label = label;
  }

  public static Host getDefault() {
    return defaultHost;
  }

  public String getName() {
    return this.name;
  }

  public Label getLabel() {
    return this.label;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }

    if (o instanceof Host) {
      Host ohost = (Host) o;
      return this.name.equals(ohost.name) && this.label.equals(ohost.label);

    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return this.name.hashCode();
  }

  @Override
  public String toString() {
    return this.name;
  }
}

package edu.cornell.cs.apl.viaduct;

/** describes an abstract machine / principal with a given trust label. */
public class Host {
  final String name;
  final Label label;

  private static final Host defaultHost = new Host("__DEFAULT__", Label.BOTTOM);

  public Host(String name, Label label) {
    this.name = name;
    this.label = label;
  }

  public String getName() {
    return this.name;
  }

  public Label getLabel() {
    return this.label;
  }

  public static Host getDefault() {
    return defaultHost;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) { return false; }

    if (o instanceof Host) {
      Host ohost = (Host)o;
      return this.name.equals(ohost.name);
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

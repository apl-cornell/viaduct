package edu.cornell.cs.apl.viaduct;

/** describes an abstract machine / principal with a given trust label. */
public class Host {
  final String name;
  final Label label;

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
}

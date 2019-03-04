package edu.cornell.cs.apl.viaduct;

/** security labels. */
public class Label {
  public static final Label BOTTOM = new Label();

  // TODO: implement
  public Label join(Label other) {
    return this;
  }

  public String toString() {
    return "{L}";
  }
}

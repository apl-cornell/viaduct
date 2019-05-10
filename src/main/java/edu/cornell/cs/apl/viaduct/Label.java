package edu.cornell.cs.apl.viaduct;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** security labels. */
public class Label implements Lattice<Label> {
  private static final Label bottom;
  private static final Label top;

  static {
    bottom = new Label();
    bottom.confidentiality.add(new HashSet<String>());

    top = new Label();
    top.integrity.add(new HashSet<String>());
  }

  protected Set<Set<String>> confidentiality;
  protected Set<Set<String>> integrity;

  protected Label() {
    this.confidentiality = new HashSet<Set<String>>();
    this.integrity = new HashSet<Set<String>>();
  }

  public Label(Set<Set<String>> conf, Set<Set<String>> integ) {
    this.confidentiality = conf;
    this.integrity = integ;
  }

  public Label(Label confLbl, Label integLbl) {
    this.confidentiality = confLbl.confidentiality;
    this.integrity = integLbl.integrity;
  }

  /** given [a,b,c,...], create label {{A,B,C,...}}. */
  public static Label and(String... strs) {
    List<String> strList = Arrays.asList(strs);
    Set<Set<String>> conf = new HashSet<Set<String>>();
    Set<String> confSingl = new HashSet<String>();
    confSingl.addAll(strList);
    conf.add(confSingl);

    Set<Set<String>> integ = new HashSet<Set<String>>();
    Set<String> integSingl = new HashSet<String>();
    integSingl.addAll(strList);
    integ.add(integSingl);

    return new Label(conf, integ);
  }

  /** conjunction of principals. */
  public static Label and(Label l1, Label l2) {
    Set<Set<String>> andConf = meetJom(l1.confidentiality, l2.confidentiality);
    Set<Set<String>> andInteg = meetJom(l1.integrity, l2.integrity);
    return new Label(andConf, andInteg);
  }

  /** given [a,b,c,...], create label {{A},{B},{C},...}. */
  public static Label or(String... strs) {
    Set<Set<String>> conf = new HashSet<Set<String>>();
    for (String str : strs) {
      Set<String> confSingl = new HashSet<String>();
      confSingl.add(str);
      conf.add(confSingl);
    }

    Set<Set<String>> integ = new HashSet<Set<String>>();
    for (String str : strs) {
      Set<String> integSingl = new HashSet<String>();
      integSingl.add(str);
      integ.add(integSingl);
    }

    return new Label(conf, integ);
  }

  /** disjunction of principals. */
  public static Label or(Label l1, Label l2) {
    Set<Set<String>> orConf = joinJom(l1.confidentiality, l2.confidentiality);
    Set<Set<String>> orInteg = joinJom(l1.integrity, l2.integrity);
    return new Label(orConf, orInteg);
  }

  public static Label bottom() {
    return bottom;
  }

  public static Label top() {
    return top;
  }

  /** confidentiality projection. */
  public Label confidentiality() {
    // Set<Set<String>> bottomInteg = Label.bottom.integrity;
    Set<Set<String>> bottomInteg = new HashSet<>();
    bottomInteg.add(new HashSet<>());
    return new Label(this.confidentiality, bottomInteg);
  }

  /** integrity projection. */
  public Label integrity() {
    // Set<Set<String>> bottomConf = Label.bottom.confidentiality;
    Set<Set<String>> bottomConf = new HashSet<>();
    bottomConf.add(new HashSet<>());
    return new Label(bottomConf, this.integrity);
  }

  protected static Set<Set<String>> normalizeJom(Set<Set<String>> jom) {
    Set<Set<String>> normalizedJom = new HashSet<>();

    for (Set<String> meet : jom) {
      boolean toAdd = true;
      Set<Set<String>> deleteSet = new HashSet<>();

      /* for meets M1 and M2, if M2 \subseteq M1 then M2 subsumes M1 */
      for (Set<String> normMeet : normalizedJom) {
        if (meet.containsAll(normMeet)) {
          toAdd = false;
          break;
        } else if (normMeet.containsAll(meet)) {
          deleteSet.add(normMeet);
        }
      }

      if (toAdd) {
        normalizedJom.add(meet);
      }
      if (deleteSet.size() > 0) {
        normalizedJom.removeAll(deleteSet);
      }
    }

    return normalizedJom;
  }

  /** join two join-of-meets together.
   * this is the canonical representation of elements of a free distributive lattice
   * read this to understand the algorithm:
   * https://en.wikipedia.org/wiki/Distributive_lattice#Free_distributive_lattices
  */
  protected static Set<Set<String>> joinJom(Set<Set<String>> jom1, Set<Set<String>> jom2) {
    Set<Set<String>> candidates = new HashSet<>();
    candidates.addAll(jom1);
    candidates.addAll(jom2);

    return normalizeJom(candidates);
  }

  /** meet two join-of-meets together.
   *  do pairwise meets, following some rewrites applying distributivity
  */
  protected static Set<Set<String>> meetJom(Set<Set<String>> jom1, Set<Set<String>> jom2) {
    Set<Set<String>> candidates = new HashSet<Set<String>>();

    for (Set<String> meet1 : jom1) {
      for (Set<String> meet2 : jom2) {
        Set<String> newMeet = new HashSet<String>();
        newMeet.addAll(meet1);
        newMeet.addAll(meet2);
        candidates.add(newMeet);
      }
    }

    return normalizeJom(candidates);
  }

  /** take join of labels in the label lattice. */
  public Label join(Label other) {
    Set<Set<String>> jconf = meetJom(this.confidentiality, other.confidentiality);
    Set<Set<String>> jinteg = joinJom(this.integrity, other.integrity);
    return new Label(jconf, jinteg);
  }

  /** take meet of labels in the label lattice. */
  public Label meet(Label other) {
    Set<Set<String>> jconf = joinJom(this.confidentiality, other.confidentiality);
    Set<Set<String>> jinteg = meetJom(this.integrity, other.integrity);
    return new Label(jconf, jinteg);
  }

  /** check flows-to for a single join-of-meets. */
  protected boolean jomFlowsTo(Set<Set<String>> jom1, Set<Set<String>> jom2) {
    // this label flows to other if for all meets M in this label,
    // M is "covered" in the other label, where there is a
    // meet that is a subset of M
    boolean flowsTo = true;
    for (Set<String> meet1 : jom1) {
      boolean covered = false;
      for (Set<String> meet2 : jom2) {
        if (meet1.containsAll(meet2)) {
          covered = true;
          break;
        }
      }

      if (!covered) {
        flowsTo = false;
        break;
      }
    }

    return flowsTo;
  }

  /** check flows-to relation (exists a path in the hesse diagram). */
  public boolean flowsTo(Label other) {
    boolean confFlowsTo = jomFlowsTo(other.confidentiality, this.confidentiality);
    boolean integFlowsTo = jomFlowsTo(this.integrity, other.integrity);
    return confFlowsTo && integFlowsTo;
  }

  /** print a join-of-meets. */
  protected String joinOfMeetStr(Set<Set<String>> jom) {
    Set<String> meetStrs = new HashSet<String>();
    for (Set<String> meet : jom) {
      meetStrs.add(String.join("&",meet));
    }
    return String.join("|", meetStrs);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) { return false; }

    if (o instanceof Label) {
      Label olbl = (Label)o;
      boolean confEq = this.confidentiality.equals(olbl.confidentiality);
      boolean integEq = this.integrity.equals(olbl.integrity);
      return confEq && integEq;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.confidentiality, this.integrity);
  }

  @Override
  public String toString() {
    String confStr = joinOfMeetStr(this.confidentiality);
    String integStr = joinOfMeetStr(this.integrity);
    if (this.confidentiality.isEmpty()) {
      return String.format("{(%s)<-}", integStr);

    } else if (this.integrity.isEmpty()) {
      return String.format("{(%s)->}", confStr);

    } else if (this.confidentiality.equals(this.integrity)) {
      return String.format("{%s}", confStr);

    } else {
      return String.format("{(%s)-> & (%s)<-}", confStr, integStr);
    }
  }
}

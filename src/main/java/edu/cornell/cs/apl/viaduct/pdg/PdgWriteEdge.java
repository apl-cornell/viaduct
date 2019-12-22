package edu.cornell.cs.apl.viaduct.pdg;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.AstPrinter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** represents writing to a variable. */
public class PdgWriteEdge<T extends AstNode> extends PdgInfoEdge<T> {
  final String commandLabel;
  final List<T> commandArgs;

  private PdgWriteEdge(PdgNode<T> source, PdgNode<T> target, String label, List<T> args) {
    super(source, target);
    this.commandLabel = label;
    this.commandArgs = args;
  }

  /** create edge b/w nodes. */
  public static <T extends AstNode> PdgWriteEdge<T> create(
      PdgNode<T> source, PdgNode<T> target, String label, List<T> args) {

    PdgWriteEdge<T> writeEdge = new PdgWriteEdge<>(source, target, label, args);
    source.addOutInfoEdge(writeEdge);
    target.addInInfoEdge(writeEdge);
    return writeEdge;
  }

  @SafeVarargs
  public static <T extends AstNode> PdgWriteEdge<T> create(
      PdgNode<T> source, PdgNode<T> target, String label, T... args) {

    return create(source, target, label, Arrays.asList(args));
  }

  @Override
  public boolean isWriteEdge() {
    return true;
  }

  @Override
  public String getLabel(AstPrinter<T> printer) {
    List<String> argStrs = new ArrayList<>();
    for (T arg : this.commandArgs) {
      argStrs.add(printer.print(arg));
    }

    String argStr = String.join(",", argStrs);
    return String.format("%s(%s)", this.commandLabel, argStr);
  }

  public List<T> getCommandArgs() {
    return this.commandArgs;
  }
}

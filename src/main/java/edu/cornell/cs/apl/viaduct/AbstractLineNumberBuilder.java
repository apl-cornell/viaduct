package edu.cornell.cs.apl.viaduct;

import java.util.Stack;

public class AbstractLineNumberBuilder {
  static class BranchInfo {
    static final String MAIN_BRANCH = "__main__";

    String marker;
    int seqNum;

    BranchInfo(String m) {
      this.marker = m;
      this.seqNum = 1;
    }

    BranchInfo() {
      this(MAIN_BRANCH);
    }

    void advanceSequence() {
      this.seqNum++;
    }

    String getMarker() {
      return this.marker;
    }

    int getSequenceNum() {
      return this.seqNum;
    }
  }

  public static final String THEN_MARKER = "then";
  public static final String ELSE_MARKER = "else";

  Stack<BranchInfo> branchStack;

  public AbstractLineNumberBuilder() {
    this.branchStack = new Stack<BranchInfo>();
    this.branchStack.push(new BranchInfo());
  }

  public void pushBranch(String marker) {
    this.branchStack.push(new BranchInfo(marker));
  }

  public void popBranch() {
    this.branchStack.pop();
  }

  /** given the current branch information, generate a line number. */
  public AbstractLineNumber generateLineNumber() {
    AbstractLineNumber lineno = new AbstractLineNumber();

    for (BranchInfo branch : this.branchStack) {
      lineno.addComponent(branch.getMarker(), branch.getSequenceNum());
    }

    this.branchStack.peek().advanceSequence();

    return lineno;
  }
}

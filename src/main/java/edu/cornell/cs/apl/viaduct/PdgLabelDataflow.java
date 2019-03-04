package edu.cornell.cs.apl.viaduct;

public class PdgLabelDataflow extends PdgDataflow<Label> {
    protected Label input(PdgNode node) {
        return node.getInLabel();
    }

    protected Label output(PdgNode node) {
        return node.getOutLabel();
    }

    protected Label transfer(PdgNode node, Label nextInput) {
        // if the node is a downgrade node, prevent transfer;
        // the out label is permanently the downgrade label
        if (node.isDowngradeNode()) {
            return node.getOutLabel();
        } else {
            return nextInput;
        }
    }

    protected void update(PdgNode node, Label nextInput, Label nextOutput) {
        node.setInLabel(nextInput);
        node.setOutLabel(nextOutput);
    }
}

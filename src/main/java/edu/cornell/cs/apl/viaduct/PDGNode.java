package edu.cornell.cs.apl.viaduct;

import java.util.Set;

public class PDGNode
{
    Set<PDGNode> inNodes;
    Set<PDGNode> outNodes;
    // only downgrade nodes have two different labels; every other node
    // only has a single label
    Label inLabel, outLabel;
    boolean isDowngrade;
    boolean isStorage;

    public PDGNode(Set<PDGNode> _inNodes, Set<PDGNode> _outNodes, Label _label)
    {
        this.inNodes = _inNodes;
        this.outNodes = _outNodes;
        this.inLabel = _label;
        this.outLabel = _label;
        this.isDowngrade = false;
        this.isStorage = false;
    }

    public PDGNode(Set<PDGNode> _inNodes, Set<PDGNode> _outNodes, Label _label, boolean _isStorage)
    {
        this.inNodes = _inNodes;
        this.outNodes = _outNodes;
        this.inLabel = _label;
        this.outLabel = _label;
        this.isDowngrade = false;
        this.isStorage = _isStorage;
    }

    public PDGNode(Set<PDGNode> _inNodes, Set<PDGNode> _outNodes, Label _inLabel, Label _outLabel)
    {
        this.inNodes = _inNodes;
        this.outNodes = _outNodes;
        this.inLabel = _inLabel;
        this.outLabel = _outLabel;
        this.isDowngrade = true;
    }

    public void addInNode(PDGNode node)
    {
        this.inNodes.add(node);
    }

    public void addOutNode(PDGNode node)
    {
        this.outNodes.add(node);
    }

    public Set<PDGNode> getInNodes()
    {
        return this.inNodes;
    }

    public Set<PDGNode> getOutNodes()
    {
        return this.outNodes;
    }

    public Label getInLabel()
    {
        return this.inLabel;
    }

    public Label getOutLabel()
    {
        return this.outLabel;
    }

    public boolean isDowngrade()
    {
        return this.isDowngrade;
    }

    public boolean isStorage()
    {
        return this.isStorage;
    }
}
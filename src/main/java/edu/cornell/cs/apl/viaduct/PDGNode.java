package edu.cornell.cs.apl.viaduct;

import java.util.Set;
import java.util.HashSet;

public abstract class PDGNode
{
    ASTNode astNode;
    Set<PDGNode> inNodes;
    Set<PDGNode> outNodes;

    public PDGNode(ASTNode _astNode, Set<PDGNode> _inNodes, Set<PDGNode> _outNodes)
    {
        this.inNodes = _inNodes;
        this.outNodes = _outNodes;
        this.astNode = _astNode;
    }

    public PDGNode(ASTNode _astNode)
    {
        this(_astNode, new HashSet<PDGNode>(), new HashSet<PDGNode>());
    }

    public void addInNode(PDGNode node)
    {
        this.inNodes.add(node);
    }

    public void addInNodes(Set<PDGNode> nodes)
    {
        this.inNodes.addAll(nodes);
    }

    public void addOutNode(PDGNode node)
    {
        this.outNodes.add(node);
    }

    public void addOutNodes(Set<PDGNode> nodes)
    {
        this.outNodes.addAll(nodes);
    }

    public Set<PDGNode> getInNodes()
    {
        return this.inNodes;
    }

    public Set<PDGNode> getOutNodes()
    {
        return this.outNodes;
    }

    public Set<PDGNode> getStorageNodeInputs()
    {
        Set<PDGNode> storageInputs = new HashSet<PDGNode>();
        for (PDGNode inNode : this.inNodes)
        {
            if (inNode.isStorageNode()) {
                storageInputs.add(inNode);

            } else {
                storageInputs.addAll(inNode.getStorageNodeInputs());
            }
        }

        return storageInputs;
    }

    public abstract Label getLabel();

    public abstract void setLabel(Label _label);

    public Label getInLabel()
    {
        return this.getLabel();
    }

    public void setInLabel(Label _label)
    {
        this.setLabel(_label);
    }

    public Label getOutLabel()
    {
        return this.getLabel();
    }

    public void setOutLabel(Label _label)
    {
        this.setLabel(_label);
    }

    public abstract boolean isStorageNode();

    public abstract boolean isComputeNode();

    public abstract boolean isDowngradeNode();
}
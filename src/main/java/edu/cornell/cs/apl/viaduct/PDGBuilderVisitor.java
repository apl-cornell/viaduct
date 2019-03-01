package edu.cornell.cs.apl.viaduct;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;

/**
 * builds a program dependency graph from an AST
 */
public class PDGBuilderVisitor implements StmtVisitor<Set<PDGNode>>, ExprVisitor<Set<PDGNode>>
{
    HashMap<Variable, PDGNode> storageNodes;
    Set<PDGNode> nodes;

    public Set<PDGNode> visit(VarLookupNode varLookup)
        throws UndeclaredVariableException
    {
        if (this.storageNodes.containsKey(varLookup.getVar())) {
            PDGNode varNode = this.storageNodes.get(varLookup.getVar());
            HashSet<PDGNode> deps = new HashSet<PDGNode>();
            deps.add(varNode);
            return deps;

        } else {
            throw new UndeclaredVariableException(varLookup.getVar());
        }
    }

    public Set<PDGNode> visit(IntLiteralNode intLit)
    {
        return new HashSet<PDGNode>();
    }

    protected Set<PDGNode> visitBinaryOp(Set<PDGNode> lhsDeps, Set<PDGNode> rhsDeps)
    {
        Set<PDGNode> deps = new HashSet<PDGNode>(lhsDeps);
        deps.addAll(rhsDeps);
        return deps;
    }

    public Set<PDGNode> visit(PlusNode plusNode)
    {
        return visitBinaryOp(plusNode.getLHS().accept(this), plusNode.getRHS().accept(this));
    }

    public Set<PDGNode> visit(BoolLiteralNode boolLit)
    {
        return new HashSet<PDGNode>();
    }

    public Set<PDGNode> visit(OrNode orNode)
    {
        return visitBinaryOp(orNode.getLHS().accept(this), orNode.getRHS().accept(this));
    }

    public Set<PDGNode> visit(AndNode andNode)
    {
        return visitBinaryOp(andNode.getLHS().accept(this), andNode.getRHS().accept(this));
    }

    public Set<PDGNode> visit(LessThanNode ltNode)
    {
        return visitBinaryOp(ltNode.getLHS().accept(this), ltNode.getRHS().accept(this));
    }

    public Set<PDGNode> visit(EqualNode eqNode)
    {
        return visitBinaryOp(eqNode.getLHS().accept(this), eqNode.getRHS().accept(this));
    }

    public Set<PDGNode> visit(LeqNode leqNode)
    {
        return visitBinaryOp(leqNode.getLHS().accept(this), leqNode.getRHS().accept(this));
    }

    public Set<PDGNode> visit(NotNode notNode)
    {
        Set<PDGNode> deps = new HashSet<PDGNode>(notNode.getNegatedExpr().accept(this));
        return deps;
    }

    public Set<PDGNode> visitDowngrade(Set<PDGNode> inNodes, Label downgradeLabel)
    {
        // create new PDG node
        // calculate inLabel later during dataflow analysis
        PDGNode node = new PDGNode(inNodes, new HashSet<PDGNode>(), Label.BOTTOM, downgradeLabel);

        // make sure to add outEdges from inNodes to the new node
        for (PDGNode inNode : inNodes)
        {
            inNode.addOutNode(node);
        }

        this.nodes.add(node);

        Set<PDGNode> deps = new HashSet<PDGNode>();
        deps.add(node);
        return deps;
    }

    public Set<PDGNode> visit(DeclassifyNode declNode)
    {
        return visitDowngrade(
                    declNode.getDeclassifiedExpr().accept(this),
                    declNode.getDowngradeLabel());
    }

    public Set<PDGNode> visit(EndorseNode endoNode)
    {
        return visitDowngrade(
                    endoNode.getEndorsedExpr().accept(this),
                    endoNode.getDowngradeLabel());
    }

    public Set<PDGNode> visit(VarDeclNode varDecl)
    {
        PDGNode pdgNode =
            new PDGNode(new HashSet<PDGNode>(),
                        new HashSet<PDGNode>(),
                        varDecl.getVarLabel(),
                        true);
        this.storageNodes.put(varDecl.getDeclaredVar(), pdgNode);
        this.nodes.add(pdgNode);

        return new HashSet<PDGNode>();
    }

    public Set<PDGNode> visit(AssignNode assignNode)
        throws UndeclaredVariableException
    {
        if (this.storageNodes.containsKey(assignNode.getVar())) {
            Set<PDGNode> inNodes = assignNode.getRHS().accept(this);

            PDGNode varNode = this.storageNodes.get(assignNode.getVar());
            Set<PDGNode> outNodes = new HashSet<PDGNode>();
            outNodes.add(varNode);

            // create new PDG node for the assignment that reads from the RHS nodes
            // and writes to the variable's storage node
            PDGNode node = new PDGNode(inNodes, outNodes, Label.BOTTOM);
            for (PDGNode inNode : inNodes)
            {
                inNode.addOutNode(node);
            }
            varNode.addInNode(node);
            this.nodes.add(node);

            Set<PDGNode> deps = new HashSet<PDGNode>();
            deps.add(node);
            return deps;

        } else {
            throw new UndeclaredVariableException(assignNode.getVar());
        }
    }

    public Set<PDGNode> visit(SeqNode seqNode)
    {
        Set<PDGNode> deps = new HashSet<PDGNode>();
        List<StmtNode> stmts = seqNode.getStmts();
        for (StmtNode stmt : stmts)
        {
            deps.addAll(stmt.accept(this));
        }

        return deps;
    }

    public Set<PDGNode> visit(IfNode ifNode)
    {
        Set<PDGNode> inNodes = ifNode.getGuard().accept(this);
        Set<PDGNode> thenNodes = ifNode.getThenBranch().accept(this);
        Set<PDGNode> elseNodes = ifNode.getElseBranch().accept(this);
        Set<PDGNode> outNodes = new HashSet<PDGNode>(thenNodes);
        outNodes.addAll(elseNodes);

        // PDG node for conditional changes PC label, so it must write
        // to storage nodes created in the then and else branches
        // furthermore, to model read channels we must add out edges to
        // all storage nodes read from in the branches
        PDGNode node = new PDGNode(inNodes, outNodes, Label.BOTTOM);
        for (PDGNode inNode : inNodes)
        {
            inNode.addOutNode(node);
        }
        for (PDGNode outNode : outNodes)
        {
            outNode.addInNode(node);

            // TODO: add edges for read channels here
        }

        Set<PDGNode> deps = new HashSet<PDGNode>();
        deps.add(node);
        return deps;
    }
}
package edu.cornell.cs.apl.viaduct;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * build program dependency graph from AST
 */
public class PDGBuilderVisitor implements StmtVisitor<Set<PDGNode>>, ExprVisitor<Set<PDGNode>>
{
    SymbolTable<Variable, PDGNode> storageNodes;
    ProgramDependencyGraph pdg;

    public PDGBuilderVisitor()
    {
        this.storageNodes = new SymbolTable<Variable, PDGNode>();
        this.pdg = new ProgramDependencyGraph();
    }

    public Set<PDGNode> visit(VarLookupNode varLookup)
    {
        if (this.storageNodes.contains(varLookup.getVar())) {
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

    protected Set<PDGNode> visitBinaryOp(BinaryExprNode binNode)
    {
        Set<PDGNode> lhsDeps = binNode.getLHS().accept(this);
        Set<PDGNode> rhsDeps = binNode.getLHS().accept(this);
        Set<PDGNode> deps = new HashSet<PDGNode>(lhsDeps);
        deps.addAll(rhsDeps);
        return deps;
    }

    public Set<PDGNode> visit(PlusNode plusNode)
    {
        return visitBinaryOp(plusNode);
    }

    public Set<PDGNode> visit(BoolLiteralNode boolLit)
    {
        return new HashSet<PDGNode>();
    }

    public Set<PDGNode> visit(OrNode orNode)
    {
        return visitBinaryOp(orNode);
    }

    public Set<PDGNode> visit(AndNode andNode)
    {
        return visitBinaryOp(andNode);
    }

    public Set<PDGNode> visit(LessThanNode ltNode)
    {
        return visitBinaryOp(ltNode);
    }

    public Set<PDGNode> visit(EqualNode eqNode)
    {
        return visitBinaryOp(eqNode);
    }

    public Set<PDGNode> visit(LeqNode leqNode)
    {
        return visitBinaryOp(leqNode);
    }

    public Set<PDGNode> visit(NotNode notNode)
    {
        Set<PDGNode> deps = new HashSet<PDGNode>(notNode.getNegatedExpr().accept(this));
        return deps;
    }

    public Set<PDGNode> visitDowngrade(ASTNode downgradeNode, Set<PDGNode> inNodes, Label downgradeLabel)
    {
        // create new PDG node
        // calculate inLabel later during dataflow analysis
        PDGNode node = new PDGComputeNode(downgradeNode, Label.BOTTOM, downgradeLabel);
        node.addInNodes(inNodes);

        // make sure to add outEdges from inNodes to the new node
        for (PDGNode inNode : inNodes)
        {
            inNode.addOutNode(node);
        }

        this.pdg.add(node);

        Set<PDGNode> deps = new HashSet<PDGNode>();
        deps.add(node);
        return deps;
    }

    public Set<PDGNode> visit(DeclassifyNode declNode)
    {
        return visitDowngrade(
                    declNode,
                    declNode.getDeclassifiedExpr().accept(this),
                    declNode.getDowngradeLabel());
    }

    public Set<PDGNode> visit(EndorseNode endoNode)
    {
        return visitDowngrade(
                    endoNode,
                    endoNode.getEndorsedExpr().accept(this),
                    endoNode.getDowngradeLabel());
    }

    public Set<PDGNode> visit(SkipNode skipNode)
    {
        return new HashSet<PDGNode>();
    }

    public Set<PDGNode> visit(VarDeclNode varDecl)
    {
        PDGNode node = new PDGStorageNode(varDecl, varDecl.getVarLabel());
        this.storageNodes.add(varDecl.getDeclaredVar(), node);
        this.pdg.add(node);

        return new HashSet<PDGNode>();
    }

    public Set<PDGNode> visit(AssignNode assignNode)
    {
        if (this.storageNodes.contains(assignNode.getVar())) {
            Set<PDGNode> inNodes = assignNode.getRHS().accept(this);
            PDGNode varNode = this.storageNodes.get(assignNode.getVar());

            // create new PDG node for the assignment that reads from the RHS nodes
            // and writes to the variable's storage node
            PDGNode node = new PDGComputeNode(assignNode, Label.BOTTOM);
            node.addInNodes(inNodes);
            node.addOutNode(varNode);

            for (PDGNode inNode : inNodes)
            {
                inNode.addOutNode(node);
            }
            varNode.addInNode(node);
            this.pdg.add(node);

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
            Set<PDGNode> stmtDeps = stmt.accept(this);
            deps.addAll(stmtDeps);
        }

        return deps;
    }

    public Set<PDGNode> visit(IfNode ifNode)
    {
        Set<PDGNode> inNodes = ifNode.getGuard().accept(this);

        // then and else branches create a new lexical scope, so
        // must push then pop a new symbol table for them

        this.storageNodes.push();
        Set<PDGNode> thenNodes = ifNode.getThenBranch().accept(this);
        this.storageNodes.pop();

        this.storageNodes.push();
        Set<PDGNode> elseNodes = ifNode.getElseBranch().accept(this);
        this.storageNodes.pop();

        Set<PDGNode> outNodes = new HashSet<PDGNode>(thenNodes);
        outNodes.addAll(elseNodes);

        // PDG node for conditional changes PC label, so it must write
        // to storage nodes created in the then and else branches
        // also, to model read channels we must add out edges to
        // all storage nodes read from the branches
        PDGNode node = new PDGComputeNode(ifNode, Label.BOTTOM);
        node.addInNodes(inNodes);
        node.addOutNodes(outNodes);

        for (PDGNode inNode : inNodes)
        {
            inNode.addOutNode(node);
        }

        Set<PDGNode> additionalOutNodes = new HashSet<PDGNode>();
        for (PDGNode outNode : outNodes)
        {
            outNode.addInNode(node);

            for (PDGNode outStorage : outNode.getStorageNodeInputs())
            {
                outStorage.addInNode(node);
                additionalOutNodes.add(outStorage);
            }
        }
        node.addOutNodes(additionalOutNodes);

        Set<PDGNode> deps = new HashSet<PDGNode>();
        deps.add(node);
        return deps;
    }

    public ProgramDependencyGraph getPDG()
    {
        return this.pdg;
    }
}

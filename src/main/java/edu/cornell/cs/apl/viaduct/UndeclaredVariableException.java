package edu.cornell.cs.apl.viaduct;

public class UndeclaredVariableException extends RuntimeException
{
    Variable var;

    public UndeclaredVariableException(Variable _var)
    {
        super();
        this.var = _var;
    }

    public Variable getVar()
    {
        return this.var;
    }
}
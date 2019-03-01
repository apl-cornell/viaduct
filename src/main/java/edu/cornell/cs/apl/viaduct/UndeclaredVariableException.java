package edu.cornell.cs.apl.viaduct;

public class UndeclaredVariableException extends Exception
{
    Variable var;

    public UndeclaredVariableException(Variable _var)
    {
        this.var = _var;
    }

    public Variable getVar()
    {
        return this.var;
    }
}
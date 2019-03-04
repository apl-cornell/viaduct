package edu.cornell.cs.apl.viaduct;

public class Label
{
    public static Label BOTTOM = new Label();

    // TODO: implement 
    public Label join(Label other)
    {
        return this;
    }

    public String toString()
    {
        return "{L}";
    }
}
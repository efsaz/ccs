package de.unisb.cs.depend.ccs_sem.lexer.tokens;


public class Equals extends AbstractToken {

    public Equals(int startPosition, int endPosition) {
        super(startPosition, endPosition);
    }

    @Override
    public String toString() {
        return "==";
    }

}

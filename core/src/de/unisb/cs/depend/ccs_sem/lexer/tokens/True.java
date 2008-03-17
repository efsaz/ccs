package de.unisb.cs.depend.ccs_sem.lexer.tokens;

import de.unisb.cs.depend.ccs_sem.lexer.tokens.categories.KeywordToken;


public class True extends KeywordToken {

    public True(int startPosition, int endPosition) {
        super(startPosition, endPosition);
    }

    @Override
    public String toString() {
        return "true";
    }

}

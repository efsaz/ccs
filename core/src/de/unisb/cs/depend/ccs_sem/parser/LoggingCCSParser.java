package de.unisb.cs.depend.ccs_sem.parser;

import java.io.Reader;
import java.util.List;

import de.unisb.cs.depend.ccs_sem.exceptions.LexException;
import de.unisb.cs.depend.ccs_sem.exceptions.ParseException;
import de.unisb.cs.depend.ccs_sem.lexer.LoggingCCSLexer;
import de.unisb.cs.depend.ccs_sem.lexer.tokens.categories.Token;
import de.unisb.cs.depend.ccs_sem.semantics.expressions.Expression;
import de.unisb.cs.depend.ccs_sem.semantics.types.Declaration;
import de.unisb.cs.depend.ccs_sem.semantics.types.Program;



public class LoggingCCSParser extends CCSParser {

    private final ParsingResult result;

    public LoggingCCSParser() {
        this(new ParsingResult());
    }

    public LoggingCCSParser(ParsingResult result) {
        if (result == null)
            throw new NullPointerException();
        this.result = result;
    }

    @Override
    public Program parse(Reader input) throws ParseException, LexException {
    	return parse(new LoggingCCSLexer(result).lex(input));
    }

    @Override
    public Program parse(String input) throws ParseException, LexException {
    	return parse(new LoggingCCSLexer(result).lex(input));
    }

    public ParsingResult getResult() {
        return result;
    }

    @Override
    public synchronized Program parse(List<Token> tokens) throws ParseException {
        result.tokens = tokens;
        return super.parse(tokens);
    }

    @Override
    protected Declaration readDeclaration(ExtendedIterator<Token> tokens)
            throws ParseException {
        final int tokenPositionBefore = tokens.nextIndex();
        final Declaration readDeclaration = super.readDeclaration(tokens);
        final int tokenPositionAfter = tokens.previousIndex();
        if (readDeclaration != null) {
            result.addDeclaration(readDeclaration, tokenPositionBefore, tokenPositionAfter);
        }
        return readDeclaration;
    }

    @Override
    protected Expression readMainExpression(ExtendedIterator<Token> tokens)
            throws ParseException {
        final int tokenPositionBefore = tokens.nextIndex();
        final Expression readExpression = super.readMainExpression(tokens);
        final int tokenPositionAfter = tokens.previousIndex();
        result.mainExpressionTokenIndexStart = tokenPositionBefore;
        result.mainExpressionTokenIndexEnd = tokenPositionAfter;
        return readExpression;
    }

}

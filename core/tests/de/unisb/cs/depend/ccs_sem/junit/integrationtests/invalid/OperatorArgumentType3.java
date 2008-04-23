package de.unisb.cs.depend.ccs_sem.junit.integrationtests.invalid;


import de.unisb.cs.depend.ccs_sem.junit.FailingIntegrationTest;


public class OperatorArgumentType3 extends FailingIntegrationTest {

    @Override
    protected String getExpressionString() {
        return "a!(!3)";
    }

    @Override
    protected int getExpectedParsingErrors() {
        return 1;
    }

    @Override
    protected boolean expectParsedProgram() {
        return false;
    }

}

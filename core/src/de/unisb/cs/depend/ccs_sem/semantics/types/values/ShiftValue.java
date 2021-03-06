package de.unisb.cs.depend.ccs_sem.semantics.types.values;

import java.util.Map;

import de.unisb.cs.depend.ccs_sem.exceptions.ArithmeticError;
import de.unisb.cs.depend.ccs_sem.semantics.types.Parameter;
import de.unisb.cs.depend.ccs_sem.semantics.types.ParameterOrProcessEqualsWrapper;


public class ShiftValue extends AbstractValue implements IntegerValue {

    // the types are checked by the parser
    private final Value left;
    private final Value right;
    private final boolean isRightShift;


    private ShiftValue(Value left, Value right, boolean isRightShift) {
        super();
        this.left = left;
        this.right = right;
        this.isRightShift = isRightShift;
    }

    public static IntegerValue create(Value left, Value right, boolean isRightShift) {
        if (left instanceof ConstIntegerValue && right instanceof ConstIntegerValue) {
            final int leftVal = ((ConstIntegerValue)left).getValue();
            final int rightVal = ((ConstIntegerValue)right).getValue();
            final int value = isRightShift ? (leftVal >> rightVal) : (leftVal << rightVal);
            return new ConstIntegerValue(value);
        }
        return new ShiftValue(left, right, isRightShift);
    }

    @Override
    public IntegerValue instantiate(Map<Parameter, Value> parameters) throws ArithmeticError {
        final Value newLeft = left.instantiate(parameters);
        final Value newRight = right.instantiate(parameters);
        if (left.equals(newLeft) && right.equals(newRight))
            return this;
        return create(newLeft, newRight, isRightShift);
    }

    public String getStringValue() {
        final boolean needParenthesisLeft = left instanceof ConditionalValue;
        final boolean needParenthesisRight = right instanceof ConditionalValue
            || right instanceof ShiftValue;
        final String leftStr = left.toString();
        final String rightStr = right.toString();
        final StringBuilder sb = new StringBuilder(leftStr.length() + rightStr.length() + 6);
        if (needParenthesisLeft)
            sb.append('(').append(leftStr).append(')');
        else
            sb.append(leftStr);
        sb.append(isRightShift ? " >> " : " << ");
        if (needParenthesisRight)
            sb.append('(').append(rightStr).append(')');
        else
            sb.append(rightStr);
        return sb.toString();
    }

    public int hashCode(Map<ParameterOrProcessEqualsWrapper, Integer> parameterOccurences) {
        final int prime = 31;
        int result = 11;
        result = prime * result + (isRightShift ? 1231 : 1237);
        result = prime * result + left.hashCode(parameterOccurences);
        result = prime * result + right.hashCode(parameterOccurences);
        return result;
    }

    public boolean equals(Object obj,
            Map<ParameterOrProcessEqualsWrapper, Integer> parameterOccurences) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final ShiftValue other = (ShiftValue) obj;
        if (isRightShift != other.isRightShift)
            return false;
        if (!left.equals(other.left, parameterOccurences))
            return false;
        if (!right.equals(other.right, parameterOccurences))
            return false;
        return true;
    }

}

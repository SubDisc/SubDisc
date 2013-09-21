package nl.liacs.subdisc;

/*
 * including this in Condition would simplify code paths in many classes
 * it would be only half a solution compared to having multiple ConditionX types
 * which would also only hold a single Column-Operator specific value field
 */
/**
 * All valid {@link ConditionBase} combinations of
 * {@link Column}.{@link AttributeType} and {@link Operator}.
 * 
 * @author marvin
 */
public enum ConditionBaseType
{
	NOMINAL_ELEMENT_OF,
	NOMINAL_EQUALS,
	NUMERIC_EQUALS,
	NUMERIC_LESS_THAN_OR_EQUAL,
	NUMERIC_GREATER_THAN_OR_EQUAL,
	NUMERIC_BETWEEN,
	BINARY_EQUALS;
}

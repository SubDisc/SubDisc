package nl.liacs.subdisc;

/*
 * Known implementing enums: AttributeType, NumericStrategy, SearchStrategy,
 * TargetType.
 */

public interface EnumInterface
{
	/**
	 * Returns a friendly <code>String<String> to show in the GUI.
	 * 
	 * @return the text <code>String</code> presented to the end user.
	 */
	@Override
	public String toString();
}

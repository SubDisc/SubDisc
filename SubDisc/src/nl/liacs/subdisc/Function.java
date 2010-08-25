/**
 * The Function class holds static methods for calculations that are shared
 * among multiple other classes.
 */
package nl.liacs.subdisc;

public class Function
{
	// for LogGamma
	private static final double GAMMA_STP = 2.50662827465;
	private static final double GAMMA_C1 = 76.18009173;
	private static final double GAMMA_C2 = -86.50532033;
	private static final double GAMMA_C3 = 24.01409822;
	private static final double GAMMA_C4 = -1.231739516;
	private static final double GAMMA_C5 = 1.20858003e-3;
	private static final double GAMMA_C6 = -5.36382e-6;

	// uninstantiable
	private Function() {}

	/**
	 * Both BinaryTable.computeBDeuFaster and CrossCube.getBDeu use this method.
	 * @param x
	 * @return
	 */
	public static double logGamma(double x)
	{
		double ser = 1.0 + GAMMA_C1 / x + GAMMA_C2 / (x + 1.0) + GAMMA_C3 / (x + 2.0) + GAMMA_C4 / (x + 3.0) + GAMMA_C5 / (x + 4.0) + GAMMA_C6 / (x + 5.0);
		return (x - 0.5) * Math.log(x + 4.5) - x - 4.5 + Math.log(GAMMA_STP * ser);
	}
}

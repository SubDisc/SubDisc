package nl.liacs.subdisc;

import java.util.Random;

public class NormalDistribution
{
	private static double itsMu;
	private static double itsSigma;
	private static Random itsRandom;
	
	// constructor for standard normal distribution
	public NormalDistribution()
	{
		itsMu = 0.0;
		itsSigma = 1.0;
		itsRandom = new Random(System.currentTimeMillis());
	}
	
	// constructor for normal distribution with general mean and variance
	public NormalDistribution(double theMean, double theVariance)
	{
		itsMu = theMean;
		itsSigma = Math.sqrt(theVariance);
		itsRandom = new Random(System.currentTimeMillis());
	}
	
	// deliver next random double from current distribution
	public double getNextDouble()
	{
		return itsSigma * itsRandom.nextGaussian() + itsMu;
	}
	
	// calculate probability density function in the point x
	public double calcPDF(double x)
	{
		return Math.pow( 
					Math.E, - Math.pow(x-itsMu,2) / (2*itsSigma*itsSigma) 
				) / ( 
					itsSigma * Math.sqrt( 2*Math.PI ) 
				);
	}
	
	// calculate cumulative distribution function in the point x, based on the error function
	public double calcCDF(double x)
	{
		return 0.5 * ( 1 + calcErf( 
									(x - itsMu) / (itsSigma * Math.sqrt(2))
								));
	}
	
	/* calculate error function using Horner's method
	   fractional error in math formula less than 1.2 * 10 ^ -7.
       although subject to catastrophic cancellation when z in very close to 0 */
    public double calcErf(double z)
    {
        double t = 1.0 / (1.0 + 0.5 * Math.abs(z));

        double ans = 1 - t * Math.exp( -z*z   -   1.26551223 +
                                            t * ( 1.00002368 +
                                            t * ( 0.37409196 +
                                            t * ( 0.09678418 +
                                            t * (-0.18628806 +
                                            t * ( 0.27886807 +
                                            t * (-1.13520398 +
                                            t * ( 1.48851587 +
                                            t * (-0.82215223 +
                                            t * ( 0.17087277))))))))));
        if (z >= 0)
        	return  ans;
        else
        	return -ans;
    }
    
	public double getMu() { return itsMu; }
	public double getSigma() { return itsSigma; }
	public double getVariance() { return itsSigma * itsSigma; }
}
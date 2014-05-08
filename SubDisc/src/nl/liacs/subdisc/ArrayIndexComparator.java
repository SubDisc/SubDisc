package nl.liacs.subdisc;

import java.util.Comparator;

//WD: Blatantly stolen from StackOverflow
public class ArrayIndexComparator implements Comparator<Integer>
{
    private final Float[] array;

    public ArrayIndexComparator(Float[] array)
    {
        this.array = array;
    }

    public Integer[] createIndexArray()
    {
        Integer[] indexes = new Integer[array.length];
        for (int i = 0; i < array.length; i++)
        {
            indexes[i] = i;
        }
        return indexes;
    }

    /*
     * So, this is decidedly flaky. I want my float array to be in descending order, 
     * and the simplest way to do that is by this cheat. PLEASE, FOR THE LOVE OF
     * EVERYTHING THAT IS HOLY, DO NOT use this comparator in any sensible context. 
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(Integer index1, Integer index2)
    {
         // from Integer to int to use as array indexes
        return array[index2].compareTo(array[index1]);
    }
}
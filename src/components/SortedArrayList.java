package components;

import java.util.*;

/**
 * A SortedArrayList stores objects in sorted order.
 * The SortedArrayList supports the add operation.
 * size is also provided, and so is a method
 * that returns an Iterator.
 * Of course a get routine could be provided too
 * as could numerous other routines..
 *
 * This example illustrates both instance inner classes
 * and static inner classes.
 * An instance inner class is used in the typical iterator pattern.
 * A static inner class is used to define a default comparator.
 */
public class SortedArrayList
{
    private ArrayList<Double> data = new ArrayList<Double>( ); // The list, maintained in sorted order
    private Comparator<Double> cmp;                    // The comparator object
        

    /**
     * Construct the SortedArrayList with specified Comparator.
     * @param compare The Comparator object.
     */
    public SortedArrayList( Comparator<Double> compare )
    {
        cmp = compare;
    }
    
    /**
     * Construct the SortedArrayList using natural ordering
     * If objects are not Comparable, an exception will be
     * thrown during an add operation.
     */
    public SortedArrayList( )
    {
        this( new DefaultComparator( ) );
    }
    
    // Test by inserting 20 Strings
    public static void main( String[] args )
    {
        SortedArrayList list = new SortedArrayList( );
        
        list.add(100.0);
        list.add(50.0);
        list.add(88.0);
        list.add(1.0);
        list.add(13.0);
        list.add(2.0);

        for(int i = 0; i <= list.size(); i++){
        	System.out.println(list.get(i));
        }
        
    }
    
    private static class DefaultComparator implements Comparator<Double>
    {
 
		@Override
		public int compare(Double arg0, Double arg1) {
			// TODO Auto-generated method stub
			return 0;
		}
    }
    
    /**
     * Add a new value to this SortedArrayList, maintaining sorted order.
     * @param x The Object to add.
     */
    public void add( Double x )
    {
        data.add( x );   // add at the end for now
        int i = data.size( ) - 1;
        
          // Slide x over to correct position
        for( ; i > 0 && cmp.compare( data.get( i - 1 ), x ) > 0; i-- )
            data.set( i, data.get( i - 1 ) );
        data.set( i, x );    
    }
    
    public double get(int i){
    	return data.get(i);
    }
    
    /**
     * Return the number of items in this SortedArrayList.
     * @return the number of items in this SortedArrayList.
     */
    public int size( )
    {
        return data.size( );
    }
    
    /**
     * Return an Iterator that can be used to traverse
     * this SortedArrayList. The remove operation is unimplemented.
     * @return An Iterator that can be used to traverse this SortedArrayList.
     */
    public Iterator iterator( )
    {
        return new SortedIterator( );
    }
    
    private class SortedIterator implements Iterator
    {
        private int current = 0;
        
        public boolean hasNext( )
        {
            return current < size( );
        }
        
        public Object next( )
        {
            return data.get( current++ );
        }
        
        public void remove( )
        {
            throw new UnsupportedOperationException( );
        }
    }
}


class TestSortedArrayList
{
    public static String listToString( SortedArrayList list )
    {
        Iterator itr = list.iterator( );
        StringBuffer sb = new StringBuffer( );
        
        for( int i = 0; itr.hasNext( ); i++ )
            sb.append( "[" + i + "]" + itr.next( ) + " " );
        return new String( sb );
    }
    
      // Test by inserting 20 Strings
    public static void main( String[] args )
    {
        SortedArrayList list = new SortedArrayList( );
        
        list.add(100.0);
        list.add(50.0);
        list.add(88.0);
        list.add(1.0);
        list.add(13.0);
        list.add(2.0);

        for(int i = 0; i <= list.size()-1; i++){
        	System.out.println(list.get(i));
        }
        
    }
} 
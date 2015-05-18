package org.gridkit.jvmtool.stacktrace;

import java.util.Collection;

public interface FlatSampleReader {

    public Collection<String> getAllFields();
    
    public boolean hasField(String field);
    
    /**
     * @return {@link String}, {@link Long}, {@link Double} or <code>null</code> value for field
     */
	public Object get(String field);
	
	/**
	 * @return <code>Sting.class</code>, <code>long.class</code>, <code>double.class</code> or <code>null</code> 
	 */
	public Class<?> getFieldType(String field);

	public long getLong(String field);

	public double getDouble(String field);

	public String getString(String field);
	
	public void copyTo(FlatSampleWriter writer);
	
    /**
     * Equivalent to 
     * <br/>
     * <code>
     * hasValue() || advance()
     * </code>
     * @return <code>this</code> 
     */
	public FlatSampleReader prime();

	/**
	 * @return <code>false</code> if reader is before start of sequence or end of sequence reached
	 */
	public boolean hasValue();
	
	public boolean advance();
	
}

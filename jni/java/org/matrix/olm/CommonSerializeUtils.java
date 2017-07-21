package org.matrix.olm;

import java.io.*;
import java.util.logging.Logger;

/**
 * Helper class dedicated to serialization mechanism (template method pattern).
 */
abstract class CommonSerializeUtils
{
	private static final Logger LOGGER = Logger.getLogger(CommonSerializeUtils.class.getName());
	
	/**
	 * Kick off the serialization mechanism.
	 *
	 * @param aOutStream output stream for serializing
	 * @throws IOException exception
	 */
	protected void serialize(ObjectOutputStream aOutStream)
			throws IOException
	{
		aOutStream.defaultWriteObject();
		
		// generate serialization key
		byte[] key = OlmUtility.getRandomKey();
		
		// compute pickle string
		StringBuffer errorMsg = new StringBuffer();
		byte[] pickledData = serialize(key, errorMsg);
		
		if (null == pickledData)
		{
			throw new OlmException(OlmException.EXCEPTION_CODE_ACCOUNT_SERIALIZATION, String.valueOf(errorMsg));
		}
		else
		{
			aOutStream.writeObject(new String(key, "UTF-8"));
			aOutStream.writeObject(new String(pickledData, "UTF-8"));
		}
	}
	
	/**
	 * Kick off the deserialization mechanism.
	 *
	 * @param aInStream input stream
	 * @throws Exception the exception
	 */
	protected void deserialize(ObjectInputStream aInStream)
			throws Exception
	{
		aInStream.defaultReadObject();
		
		String keyAsString = (String) aInStream.readObject();
		String pickledDataAsString = (String) aInStream.readObject();
		
		byte[] key;
		byte[] pickledData;
		
		try
		{
			key = keyAsString.getBytes("UTF-8");
			pickledData = pickledDataAsString.getBytes("UTF-8");
			
			deserialize(pickledData, key);
		}
		catch (Exception e)
		{
			throw new OlmException(OlmException.EXCEPTION_CODE_ACCOUNT_DESERIALIZATION, e.getMessage());
		}
		
		LOGGER.info("## deserializeObject(): success");
	}
	
	protected abstract byte[] serialize(byte[] aKey, StringBuffer aErrorMsg);
	
	protected abstract void deserialize(byte[] aSerializedData, byte[] aKey)
			throws Exception;
}

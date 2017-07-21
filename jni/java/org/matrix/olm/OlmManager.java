package org.matrix.olm;

import java.io.*;
import java.util.logging.Logger;

import cz.adamh.utils.NativeUtils;

/**
 * Olm SDK entry point class.<br> An OlmManager instance must be created at first to enable native library load.
 * <br><br>Detailed implementation guide is available at <a href="http://matrix.org/docs/guides/e2e_implementation.html">Implementing End-to-End Encryption in Matrix clients</a>.
 */
public class OlmManager
{
	private static final Logger LOGGER = Logger.getLogger(OlmManager.class.getName());
	
	/**
	 * Constructor.
	 */
	public OlmManager()
	{
		try
		{
			NativeUtils.loadLibraryFromJar("/libolm.so");
			NativeUtils.loadLibraryFromJar("/libolmjava.so");
		}
		catch (IOException e)
		{
			LOGGER.severe("Exception loadLibrary() - Msg=" + e.getMessage());
		}
	}
	
	/**
	 * Provide the native OLM lib version.
	 *
	 * @return the lib version as a string
	 */
	public String getOlmLibVersion()
	{
		return getOlmLibVersionJni();
	}
	
	private native String getOlmLibVersionJni();
}


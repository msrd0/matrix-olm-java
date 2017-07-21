package org.matrix.olm;

/**
 * Message class used in Olm sessions to contain the encrypted data.<br>
 * See {@link OlmSession#decryptMessage(OlmMessage)} and {@link OlmSession#encryptMessage(String)}.
 * <br>Detailed implementation guide is available at <a href="http://matrix.org/docs/guides/e2e_implementation.html">Implementing End-to-End Encryption in Matrix clients</a>.
 */
public class OlmMessage
{
	/** PRE KEY message type (used to establish new Olm session) **/
	public final static int MESSAGE_TYPE_PRE_KEY = 0;
	/** normal message type **/
	public final static int MESSAGE_TYPE_MESSAGE = 1;
	
	/** the encrypted message **/
	public String mCipherText;
	
	/** defined by {@link #MESSAGE_TYPE_MESSAGE} or {@link #MESSAGE_TYPE_PRE_KEY} **/
	public long mType;
}

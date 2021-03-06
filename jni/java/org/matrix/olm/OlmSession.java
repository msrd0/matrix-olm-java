/*
 * Copyright 2016 OpenMarket Ltd
 * Copyright 2016 Vector Creations Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.matrix.olm;

import static java.nio.charset.StandardCharsets.*;
import static org.matrix.olm.OlmException.*;

import java.io.*;
import javax.annotation.*;

import org.slf4j.*;

/**
 * Session class used to create Olm sessions in conjunction with {@link OlmAccount} class.<br>
 * Olm session is used to encrypt data between devices, especially to create Olm group sessions (see {@link OlmOutboundGroupSession} and {@link OlmInboundGroupSession}).<br>
 * To establish an Olm session with Bob, Alice calls {@link #initOutboundSession(OlmAccount, String, String)} with Bob's identity and onetime keys. Then Alice generates an encrypted PRE_KEY message ({@link #encryptMessage(String)})
 * used by Bob to open the Olm session in his side with {@link #initOutboundSession(OlmAccount, String, String)}.
 * From this step on, messages can be exchanged by using {@link #encryptMessage(String)} and {@link #decryptMessage(OlmMessage)}.
 * <br><br>Detailed implementation guide is available at <a href="http://matrix.org/docs/guides/e2e_implementation.html">Implementing End-to-End Encryption in Matrix clients</a>.
 */
public class OlmSession extends CommonSerializeUtils implements Serializable
{
	private static final long serialVersionUID = -8975488639186976419L;
	private static final Logger LOGGER = LoggerFactory.getLogger(OlmSession.class);
	
	/**
	 * Session Id returned by JNI.
	 * This value uniquely identifies the native session instance.
	 **/
	private transient long mNativeId;
	
	public OlmSession()
			throws OlmException
	{
		try
		{
			mNativeId = createNewSessionJni();
		}
		catch (Exception e)
		{
			throw new OlmException(EXCEPTION_CODE_INIT_SESSION_CREATION, e.getMessage());
		}
	}
	
	/**
	 * Create an OLM session in native side.<br>
	 * Do not forget to call {@link #releaseSession()} when JAVA side is done.
	 *
	 * @return native account instance identifier or throw an exception.
	 */
	private native long createNewSessionJni();
	
	/**
	 * Getter on the session ID.
	 *
	 * @return native session ID
	 */
	long getOlmSessionId()
	{
		return mNativeId;
	}
	
	/**
	 * Destroy the corresponding OLM session native object.<br>
	 * This method must ALWAYS be called when this JAVA instance
	 * is destroyed (ie. garbage collected) to prevent memory leak in native side.
	 * See {@link #createNewSessionJni()}.
	 */
	private native void releaseSessionJni();
	
	/**
	 * Release native session and invalid its JAVA reference counter part.<br>
	 * Public API for {@link #releaseSessionJni()}.
	 */
	public void releaseSession()
	{
		if (0 != mNativeId)
		{
			releaseSessionJni();
		}
		mNativeId = 0;
	}
	
	/**
	 * Return true the object resources have been released.<br>
	 *
	 * @return true the object resources have been released
	 */
	public boolean isReleased()
	{
		return (0 == mNativeId);
	}
	
	/**
	 * Creates a new out-bound session for sending messages to a recipient
	 * identified by an identity key and a one time key.<br>
	 *
	 * @param aAccount          the account to associate with this session
	 * @param aTheirIdentityKey the identity key of the recipient
	 * @param aTheirOneTimeKey  the one time key of the recipient
	 * @throws OlmException the failure reason
	 */
	public void initOutboundSession(@Nonnull OlmAccount aAccount,
									@Nonnull String aTheirIdentityKey, @Nonnull String aTheirOneTimeKey)
			throws OlmException
	{
		if (aTheirIdentityKey.isEmpty() || aTheirOneTimeKey.isEmpty())
		{
			LOGGER.error("## initOutboundSession(): invalid input parameters");
			throw new OlmException(EXCEPTION_CODE_SESSION_INIT_OUTBOUND_SESSION, "invalid input parameters");
		}
		else
		{
			try
			{
				initOutboundSessionJni(aAccount.getOlmAccountId(),
						aTheirIdentityKey.getBytes(UTF_8), aTheirOneTimeKey.getBytes(UTF_8));
			}
			catch (Exception e)
			{
				LOGGER.error("## initOutboundSession(): " + e.getMessage());
				throw new OlmException(EXCEPTION_CODE_SESSION_INIT_OUTBOUND_SESSION, e.getMessage());
			}
		}
	}
	
	/**
	 * Create a new in-bound session for sending/receiving messages from an
	 * incoming PRE_KEY message.<br> The recipient is defined as the entity
	 * with whom the session is established.
	 * An exception is thrown if the operation fails.
	 *
	 * @param aOlmAccountId     account instance
	 * @param aTheirIdentityKey the identity key of the recipient
	 * @param aTheirOneTimeKey  the one time key of the recipient
	 **/
	private native void initOutboundSessionJni(long aOlmAccountId, byte[] aTheirIdentityKey, byte[] aTheirOneTimeKey);
	
	/**
	 * Create a new in-bound session for sending/receiving messages from an
	 * incoming PRE_KEY message ({@link OlmMessage#MESSAGE_TYPE_PRE_KEY}).<br>
	 * This API may be used to process a "m.room.encrypted" event when type = 1 (PRE_KEY).
	 *
	 * @param aAccount   the account to associate with this session
	 * @param aPreKeyMsg PRE KEY message
	 * @throws OlmException the failure reason
	 */
	public void initInboundSession(@Nonnull OlmAccount aAccount, @Nonnull String aPreKeyMsg)
			throws OlmException
	{
		if (aPreKeyMsg.isEmpty())
		{
			LOGGER.error("## initInboundSession(): invalid input parameters");
			throw new OlmException(OlmException.EXCEPTION_CODE_SESSION_INIT_INBOUND_SESSION, "invalid input parameters");
		}
		else
		{
			try
			{
				initInboundSessionJni(aAccount.getOlmAccountId(), aPreKeyMsg.getBytes("UTF-8"));
			}
			catch (Exception e)
			{
				LOGGER.error("## initInboundSession(): " + e.getMessage());
				throw new OlmException(OlmException.EXCEPTION_CODE_SESSION_INIT_INBOUND_SESSION, e.getMessage());
			}
		}
	}
	
	/**
	 * Create a new in-bound session for sending/receiving messages from an
	 * incoming PRE_KEY message.<br>
	 * An exception is thrown if the operation fails.
	 *
	 * @param aOlmAccountId  account instance
	 * @param aOneTimeKeyMsg PRE_KEY message
	 */
	private native void initInboundSessionJni(long aOlmAccountId, byte[] aOneTimeKeyMsg);
	
	/**
	 * Create a new in-bound session for sending/receiving messages from an
	 * incoming PRE_KEY({@link OlmMessage#MESSAGE_TYPE_PRE_KEY}) message based on the sender identity key.<br>
	 * Public API for {@link #initInboundSessionFromIdKeyJni(long, byte[], byte[])}.
	 * This API may be used to process a "m.room.encrypted" event when type = 1 (PRE_KEY).
	 * This method must only be called the first time a pre-key message is received from an inbound session.
	 *
	 * @param aAccount          the account to associate with this session
	 * @param aTheirIdentityKey the sender identity key
	 * @param aPreKeyMsg        PRE KEY message
	 * @throws OlmException the failure reason
	 */
	public void initInboundSessionFrom(@Nonnull OlmAccount aAccount,
									   @Nonnull String aTheirIdentityKey, @Nonnull String aPreKeyMsg)
			throws OlmException
	{
		if (aPreKeyMsg.isEmpty())
		{
			LOGGER.error("## initInboundSessionFrom(): invalid input parameters");
			throw new OlmException(EXCEPTION_CODE_SESSION_INIT_INBOUND_SESSION_FROM, "invalid input parameters");
		}
		else
		{
			try
			{
				initInboundSessionFromIdKeyJni(aAccount.getOlmAccountId(),
						aTheirIdentityKey.getBytes(UTF_8), aPreKeyMsg.getBytes(UTF_8));
			}
			catch (Exception e)
			{
				LOGGER.error("## initInboundSessionFrom(): " + e.getMessage());
				throw new OlmException(EXCEPTION_CODE_SESSION_INIT_INBOUND_SESSION_FROM, e.getMessage());
			}
		}
	}
	
	/**
	 * Create a new in-bound session for sending/receiving messages from an
	 * incoming PRE_KEY message based on the recipient identity key.<br>
	 * An exception is thrown if the operation fails.
	 *
	 * @param aOlmAccountId     account instance
	 * @param aTheirIdentityKey the identity key of the recipient
	 * @param aOneTimeKeyMsg    encrypted message
	 */
	private native void initInboundSessionFromIdKeyJni(long aOlmAccountId, byte[] aTheirIdentityKey, byte[] aOneTimeKeyMsg);
	
	/**
	 * Get the session identifier.<br> Will be the same for both ends of the
	 * conversation. The session identifier is returned as a String object.
	 * Session Id sample: "session_id":"M4fOVwD6AABrkTKl"
	 * Public API for {@link #getSessionIdentifierJni()}.
	 *
	 * @return the session ID
	 * @throws OlmException the failure reason
	 */
	@Nonnull
	public String sessionIdentifier()
			throws OlmException
	{
		byte buffer[];
		
		try
		{
			buffer = getSessionIdentifierJni();
			
			if (buffer == null)
				throw new Exception("getSessionIdentifierJni()=null");
		}
		catch (Exception e)
		{
			LOGGER.error("## sessionIdentifier(): " + e.getMessage());
			throw new OlmException(EXCEPTION_CODE_SESSION_SESSION_IDENTIFIER, e.getMessage());
		}
		
		return new String(buffer, UTF_8);
	}
	
	/**
	 * Get the session identifier for this session.
	 * An exception is thrown if the operation fails.
	 *
	 * @return the session identifier
	 */
	private native byte[] getSessionIdentifierJni();
	
	/**
	 * Checks if the PRE_KEY({@link OlmMessage#MESSAGE_TYPE_PRE_KEY}) message is for this in-bound session.<br>
	 * This API may be used to process a "m.room.encrypted" event when type = 1 (PRE_KEY).
	 * Public API for {@link #matchesInboundSessionJni(byte[])}.
	 *
	 * @param aOneTimeKeyMsg PRE KEY message
	 * @return true if the one time key matches.
	 */
	public boolean matchesInboundSession(@Nonnull String aOneTimeKeyMsg)
	{
		boolean retCode = false;
		
		try
		{
			retCode = matchesInboundSessionJni(aOneTimeKeyMsg.getBytes(UTF_8));
		}
		catch (Exception e)
		{
			LOGGER.error("## matchesInboundSession(): failed " + e.getMessage());
		}
		
		return retCode;
	}
	
	/**
	 * Checks if the PRE_KEY message is for this in-bound session.<br>
	 * This API may be used to process a "m.room.encrypted" event when type = 1 (PRE_KEY).
	 * An exception is thrown if the operation fails.
	 *
	 * @param aOneTimeKeyMsg PRE KEY message
	 * @return true if the PRE_KEY message matches
	 */
	private native boolean matchesInboundSessionJni(byte[] aOneTimeKeyMsg);
	
	/**
	 * Checks if the PRE_KEY({@link OlmMessage#MESSAGE_TYPE_PRE_KEY}) message is for this in-bound session based on the sender identity key.<br>
	 * This API may be used to process a "m.room.encrypted" event when type = 1 (PRE_KEY).
	 * Public API for {@link #matchesInboundSessionJni(byte[])}.
	 *
	 * @param aTheirIdentityKey the sender identity key
	 * @param aOneTimeKeyMsg    PRE KEY message
	 * @return this if operation succeed, null otherwise
	 */
	public boolean matchesInboundSessionFrom(@Nonnull String aTheirIdentityKey, @Nonnull String aOneTimeKeyMsg)
	{
		boolean retCode = false;
		
		try
		{
			retCode = matchesInboundSessionFromIdKeyJni(aTheirIdentityKey.getBytes(UTF_8), aOneTimeKeyMsg.getBytes(UTF_8));
		}
		catch (Exception e)
		{
			LOGGER.error("## matchesInboundSessionFrom(): failed " + e.getMessage());
		}
		
		return retCode;
	}
	
	/**
	 * Checks if the PRE_KEY message is for this in-bound session based on the sender identity key.<br>
	 * This API may be used to process a "m.room.encrypted" event when type = 1 (PRE_KEY).
	 * An exception is thrown if the operation fails.
	 *
	 * @param aTheirIdentityKey the identity key of the sender
	 * @param aOneTimeKeyMsg    PRE KEY message
	 * @return true if the PRE_KEY message matches.
	 */
	private native boolean matchesInboundSessionFromIdKeyJni(byte[] aTheirIdentityKey, byte[] aOneTimeKeyMsg);
	
	/**
	 * Encrypt a message using the session.<br>
	 * The encrypted message is returned in a OlmMessage object.
	 * Public API for {@link #encryptMessageJni(byte[])}.
	 *
	 * @param aClearMsg message to encrypted
	 * @return the encrypted message
	 * @throws OlmException the failure reason
	 */
	@Nonnull
	public OlmMessage encryptMessage(@Nonnull String aClearMsg)
			throws OlmException
	{
		try
		{
			return encryptMessageJni(aClearMsg.getBytes(UTF_8));
		}
		catch (Exception e)
		{
			LOGGER.error("## encryptMessage(): failed " + e.getMessage());
			throw new OlmException(EXCEPTION_CODE_SESSION_ENCRYPT_MESSAGE, e.getMessage());
		}
	}
	
	/**
	 * Encrypt a message using the session.<br>
	 * An exception is thrown if the operation fails.
	 *
	 * @param aClearMsg     clear text message
	 * @return the encrypted message
	 */
	private native OlmMessage encryptMessageJni(byte[] aClearMsg);
	
	/**
	 * Decrypt a message using the session.<br>
	 * The encrypted message is given as a OlmMessage object.
	 *
	 * @param aEncryptedMsg message to decrypt
	 * @return the decrypted message
	 * @throws OlmException the failure reason
	 */
	@Nonnull
	public String decryptMessage(@Nonnull OlmMessage aEncryptedMsg)
			throws OlmException
	{
		try
		{
			return new String(decryptMessageJni(aEncryptedMsg.getCipherText().getBytes(UTF_8), aEncryptedMsg.getType()), UTF_8);
		}
		catch (Exception e)
		{
			LOGGER.error("## decryptMessage(): failed " + e.getMessage());
			throw new OlmException(EXCEPTION_CODE_SESSION_DECRYPT_MESSAGE, e.getMessage());
		}
	}
	
	/**
	 * Decrypt a message using the session.<br>
	 * An exception is thrown if the operation fails.
	 *
	 * @param aEncryptedMsg message to decrypt
	 * @param aEncryptedMsgType the message type
	 * @return the decrypted message
	 */
	private native byte[] decryptMessageJni(byte[] aEncryptedMsg, int aEncryptedMsgType);
	
	//==============================================================================================================
	// Serialization management
	//==============================================================================================================
	
	/**
	 * Kick off the serialization mechanism.
	 *
	 * @param aOutStream output stream for serializing
	 * @throws IOException exception
	 */
	private void writeObject(@Nonnull ObjectOutputStream aOutStream)
			throws IOException
	{
		serialize(aOutStream);
	}
	
	/**
	 * Kick off the deserialization mechanism.
	 *
	 * @param aInStream input stream
	 * @throws IOException            exception
	 * @throws ClassNotFoundException exception
	 */
	private void readObject(@Nonnull ObjectInputStream aInStream)
			throws Exception
	{
		deserialize(aInStream);
	}
	
	/**
	 * Return a session as a bytes buffer.<br>
	 * The account is serialized and encrypted with aKey.
	 * In case of failure, an error human readable
	 * description is provide in aErrorMsg.
	 *
	 * @param aKey      encryption key
	 * @param aErrorMsg error message description
	 * @return session as a bytes buffer
	 */
	@Override
	@Nullable
	protected byte[] serialize(@Nonnull byte[] aKey, @Nonnull StringBuffer aErrorMsg)
	{
		byte[] pickleRetValue = null;
		
		// sanity check
		aErrorMsg.setLength(0);
		try
		{
			pickleRetValue = serializeJni(aKey);
		}
		catch (Exception e)
		{
			LOGGER.error("## serializeDataWithKey(): failed " + e.getMessage());
			aErrorMsg.append(e.getMessage());
		}
		
		return pickleRetValue;
	}
	
	/**
	 * Serialize and encrypt session instance.<br>
	 * An exception is thrown if the operation fails.
	 *
	 * @param aKeyBuffer key used to encrypt the serialized account data
	 * @return the serialised account as bytes buffer.
	 **/
	private native byte[] serializeJni(byte[] aKeyBuffer);
	
	/**
	 * Loads an account from a pickled base64 string.<br>
	 * See {@link #serialize(byte[], StringBuffer)}
	 *
	 * @param aSerializedData pickled account in a base64 string format
	 * @param aKey            key used to encrypted
	 */
	@Override
	protected void deserialize(@Nonnull byte[] aSerializedData, @Nonnull byte[] aKey)
			throws Exception
	{
		String errorMsg = null;
		
		try
		{
			mNativeId = deserializeJni(aSerializedData, aKey);
		}
		catch (Exception e)
		{
			LOGGER.error("## deserialize() failed " + e.getMessage());
			errorMsg = e.getMessage();
		}
		
		if (errorMsg != null)
		{
			releaseSession();
			throw new OlmException(EXCEPTION_CODE_ACCOUNT_DESERIALIZATION, errorMsg);
		}
	}
	
	/**
	 * Allocate a new session and initialize it with the serialisation data.<br>
	 * An exception is thrown if the operation fails.
	 *
	 * @param aSerializedData the session serialisation buffer
	 * @param aKey            the key used to encrypt the serialized account data
	 * @return the deserialized session
	 **/
	private native long deserializeJni(byte[] aSerializedData, byte[] aKey);
	
	
	/**
	 * Hash code of the session identifier.
	 */
	@Override
	public int hashCode()
	{
		try
		{
			return sessionIdentifier().hashCode();
		}
		catch (OlmException ex)
		{
			LOGGER.error("Error while receiving session identifier", ex);
			return super.hashCode();
		}
	}
	
	/**
	 * Compare the session identifier of this session with <code>other</code>.
	 */
	@Override
	public boolean equals(Object other)
	{
		if (!(other instanceof OlmSession))
			return false;
		OlmSession o = (OlmSession) other;
		try
		{
			return sessionIdentifier().equals(o.sessionIdentifier());
		}
		catch (OlmException ex)
		{
			LOGGER.error("Error while receiving session identifier", ex);
			return super.equals(other);
		}
	}
}


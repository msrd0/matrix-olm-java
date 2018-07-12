/*
 * Copyright 2018 New Vector Ltd
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

import static org.matrix.olm.OlmException.*;

import javax.annotation.Nonnull;

import org.slf4j.*;

public class OlmPkEncryption
{
	private static final Logger LOGGER = LoggerFactory.getLogger(OlmPkEncryption.class);
	
	/**
	 * Session Id returned by JNI.
	 * This value uniquely identifies the native session instance.
	 **/
	private transient long mNativeId;
	
	public OlmPkEncryption()
			throws OlmException
	{
		try
		{
			mNativeId = createNewPkEncryptionJni();
		}
		catch (Exception e)
		{
			throw new OlmException(OlmException.EXCEPTION_CODE_PK_ENCRYPTION_CREATION, e.getMessage());
		}
	}
	
	private native long createNewPkEncryptionJni();
	
	private native void releasePkEncryptionJni();
	
	public void releaseEncryption()
	{
		if (0 != mNativeId)
			releasePkEncryptionJni();
		mNativeId = 0;
	}
	
	public boolean isReleased()
	{
		return (0 == mNativeId);
	}
	
	public void setRecipientKey(@Nonnull String aKey)
			throws OlmException
	{
		try
		{
			setRecipientKeyJni(aKey.getBytes("UTF-8"));
		}
		catch (Exception e)
		{
			LOGGER.error("## setRecipientKey(): failed " + e.getMessage());
			throw new OlmException(EXCEPTION_CODE_PK_ENCRYPTION_SET_RECIPIENT_KEY, e.getMessage());
		}
	}
	
	private native void setRecipientKeyJni(byte[] aKey);
	
	public OlmPkMessage encrypt(@Nonnull String aPlaintext)
			throws OlmException
	{
		@Nonnull
		final OlmPkMessage encryptedMsgRetValue = new OlmPkMessage();
		
		try
		{
			byte[] ciphertextBuffer = encryptJni(aPlaintext.getBytes("UTF-8"), encryptedMsgRetValue);
			
			if (null != ciphertextBuffer)
			{
				encryptedMsgRetValue.mCipherText = new String(ciphertextBuffer, "UTF-8");
			}
		}
		catch (Exception e)
		{
			LOGGER.error("## pkEncrypt(): failed " + e.getMessage());
			throw new OlmException(EXCEPTION_CODE_PK_ENCRYPTION_ENCRYPT, e.getMessage());
		}
		
		return encryptedMsgRetValue;
	}
	
	private native byte[] encryptJni(byte[] plaintext, @Nonnull OlmPkMessage aMessage);
}

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

import static kotlin.text.Charsets.UTF_8;
import static org.matrix.olm.OlmException.*;

import javax.annotation.Nonnull;

import org.slf4j.*;

public class OlmPkDecryption
{
	private static final Logger LOGGER = LoggerFactory.getLogger(OlmPkDecryption.class);
	
	/**
	 * Session Id returned by JNI.
	 * This value uniquely identifies the native session instance.
	 **/
	private transient long mNativeId;
	
	public OlmPkDecryption()
			throws OlmException
	{
		try
		{
			mNativeId = createNewPkDecryptionJni();
		}
		catch (Exception e)
		{
			throw new OlmException(OlmException.EXCEPTION_CODE_PK_DECRYPTION_CREATION, e.getMessage());
		}
	}
	
	private native long createNewPkDecryptionJni();
	
	private native void releasePkDecryptionJni();
	
	public void releaseDecryption()
	{
		if (0 != mNativeId)
			releasePkDecryptionJni();
		mNativeId = 0;
	}
	
	public boolean isReleased()
	{
		return (0 == mNativeId);
	}
	
	@Nonnull
	public String generateKey()
			throws OlmException
	{
		try
		{
			byte[] key = generateKeyJni();
			return new String(key, "UTF-8");
		}
		catch (Exception e)
		{
			LOGGER.error("## setRecipientKey(): failed " + e.getMessage());
			throw new OlmException(EXCEPTION_CODE_PK_DECRYPTION_GENERATE_KEY, e.getMessage());
		}
	}
	
	private native byte[] generateKeyJni();
	
	@Nonnull
	public String decrypt(@Nonnull OlmPkMessage aMessage)
			throws OlmException
	{
		try
		{
			return new String(decryptJni(aMessage), UTF_8);
		}
		catch (Exception e)
		{
			LOGGER.error("## pkDecrypt(): failed " + e.getMessage());
			throw new OlmException(EXCEPTION_CODE_PK_DECRYPTION_DECRYPT, e.getMessage());
		}
	}
	
	private native byte[] decryptJni(@Nonnull OlmPkMessage aMessage);
}

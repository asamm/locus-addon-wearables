package com.asamm.locus.addon.wear.common.communication;

import java.io.IOException;
import java.io.InputStream;

import locus.api.objects.Storable;
import locus.api.utils.Logger;

/**
 * Created by Milan Cejnar on 08.03.2018.
 * Asamm Software, s.r.o.
 */

public class StorableStreamReader {
	private static final int HEADER_LEN = 8;
	private static final int MAX_BUFFER_SIZE = 8 * 1024 * 1024;
	private int mBufferSize = 64 * 1024;
	private byte[] mBuffer;
	private final InputStream mIs;

	public StorableStreamReader(InputStream inputStream) {
		mIs = inputStream;
		mBuffer = new byte[mBufferSize];
	}

	public <E extends Storable> E read(Class<E> clazz) throws IOException {
		int len = 0;
		// first read out first 8 bytes of Storable header
		while (len >= 0 && len < HEADER_LEN) {
			int tmp = mIs.read(mBuffer, len, HEADER_LEN - len);
			len = (tmp >= 0) ? len + tmp : -1;
		}
		if (len < 0)
			throw new IOException("Reached EOF while reading Storable");

		int size = readInt(HEADER_LEN);
		int read = 0;

		// try to enlarge buffer if the data is larger
		if (size + HEADER_LEN > mBufferSize) {
			byte[] tmpHeaderBuff = new byte[HEADER_LEN];
			for (int i = 0; i < HEADER_LEN; i++) {
				tmpHeaderBuff[i] = mBuffer[i];
			}
			Logger.logD("STORABLE READER", "increasing buffer");
			while (size + HEADER_LEN > mBufferSize && mBufferSize <= MAX_BUFFER_SIZE) {
				mBufferSize <<= 1;
			}
			if (mBufferSize > MAX_BUFFER_SIZE) {
				throw new IllegalArgumentException("Storable read failed, Storable too large with size of " + size + " B.");
			}
			mBuffer = new byte[mBufferSize];

			for (int i = 0; i < HEADER_LEN; i++) {
				mBuffer[i] = tmpHeaderBuff[i];
			}
		}
		// read storable body
		while (size - read > 0 && read >= 0) {
			int tmp = mIs.read(mBuffer, len + read, size - read);
			read = (tmp >= 0) ? read + tmp : -1;
		}
		// create and return storable
		if (read < 0)
			throw new IOException("Reached EOF while reading Storable");
		try {
			E result = (E) clazz.getConstructor(byte[].class).newInstance(mBuffer);
			return result;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private int readInt(int position) {
		return mBuffer[position - 4] << 24 |
				(mBuffer[position - 3] & 0xff) << 16 |
				(mBuffer[position - 2] & 0xff) << 8 |
				(mBuffer[position - 1] & 0xff);
	}
}

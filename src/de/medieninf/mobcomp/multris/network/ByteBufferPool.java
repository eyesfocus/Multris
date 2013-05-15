package de.medieninf.mobcomp.multris.network;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.util.Log;

public class ByteBufferPool {

	private Map<Integer, ArrayList<byte[]>> pool;
	int leasedSize;
	int internalSize;

	public ByteBufferPool() {
		pool = new ConcurrentHashMap<Integer, ArrayList<byte[]>>();
		leasedSize = 0;
		internalSize = 0;
	}

	public byte[] get(Integer size) {
		logMemoryUsed();
		this.leasedSize += size;
		if (pool.containsKey(size)) {
			ArrayList<byte[]> bytes = pool.get(size);
			synchronized (bytes) {
				if (!bytes.isEmpty()) {
					byte[] ret = bytes.remove(0);
					this.internalSize -= size;
					return ret;
				}
			}
		}
		return new byte[size];
	}

	public void recycle(byte[] buffer) {
		this.leasedSize -= buffer.length;
		this.internalSize += buffer.length;
		if (!pool.containsKey(buffer.length)) {
			pool.put(buffer.length, new ArrayList<byte[]>());
		}
		ArrayList<byte[]> bytes = pool.get(buffer.length);
		synchronized (bytes) {
			bytes.add(buffer);
		}
	}

	public void clear() {
		pool.clear();
	}

	private void logMemoryUsed() {
		Log.v("ByteBufferPool", "Memory used " + leasedSize + "/" + internalSize + " total = " + (leasedSize + internalSize));
	}

}

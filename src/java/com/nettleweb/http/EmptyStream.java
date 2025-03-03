package com.nettleweb.http;

import org.jetbrains.annotations.*;

import java.io.*;

public class EmptyStream extends InputStream {
	public EmptyStream() {
	}

	@Override
	public int read() {
		return -1;
	}

	@Override
	public int read(@NotNull byte[] b) {
		return -1;
	}

	@Override
	public int read(@NotNull byte[] b, int off, int len) {
		return -1;
	}

	@Override
	public long skip(long n) {
		return 0L;
	}

	@Override
	public void close() {
	}

	@Override
	public long transferTo(OutputStream out) {
		return 0L;
	}

	@NotNull
	@Override
	public byte[] readNBytes(int len) {
		return new byte[0];
	}

	@Override
	public int readNBytes(byte[] b, int off, int len) {
		return 0;
	}

	@NotNull
	@Override
	public byte[] readAllBytes() {
		return new byte[0];
	}
}

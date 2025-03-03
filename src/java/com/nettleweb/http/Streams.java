package com.nettleweb.http;

import org.jetbrains.annotations.*;

import java.io.*;
import java.lang.Cloneable;
import java.nio.charset.*;
import java.util.*;

final class Streams {
	private Streams() {}

	public static long pipe(InputStream is, OutputStream os) throws IOException {
		byte[] buf = new byte[8192];
		long k = 0L;
		int i;

		while ((i = is.read(buf, 0, 8192)) >= 0) {
			os.write(buf, 0, i);
			os.flush();
			k += i;
		}

		return k;
	}

	@NotNull
	public static byte[] readUntil(InputStream is, byte[] mark, int maxLength) throws IOException {
		byte[] data = new byte[0];
		int length = 0;

loop:
		while (true) {
			for (int i = 0; i < mark.length; i++) {
				if ((length++) >= maxLength) {
					System.out.println(Arrays.toString(data));
					throw new IOException("Maximum buffer size exceeded: " + length);
				}

				final int b = is.read();
				if (b < 0)
					throw new EOFException();

				if (b != mark[i]) {
					final int len = data.length;
					byte[] merged = new byte[length];

					System.arraycopy(data, 0, merged, 0, len);
					if (i > 0)
						System.arraycopy(mark, 0, merged, len, i);

					merged[length - 1] = (byte) b;
					data = merged;
					continue loop;
				}
			}
			return data;
		}
	}

	public static byte[] readNBytes(InputStream is, int len) throws IOException {
		byte[] buffer = new byte[8192];
		byte[] outBuf = new byte[0];

		while (len > 0) {
			int i = is.read(buffer, 0, Math.min(8192, len));
			if (i < 0)
				break;

			int length = outBuf.length;
			byte[] buf = new byte[length + i];
			System.arraycopy(outBuf, 0, buf, 0, length);
			System.arraycopy(buffer, 0, buf, length, i);
			outBuf = buf;
			len -= i;
		}

		return outBuf;
	}

	public static void closeUnchecked(Closeable stream) {
		try {
			stream.close();
		} catch (Exception e) {
			// ignore
		}
	}

	public static final class UnsupportedStream extends CompatStream {
		private final Object stream;

		public UnsupportedStream(@NotNull Object stream) {
			this.stream = stream;
		}

		@Override
		public int read(@NotNull byte[] b, int off, int len) throws IOException {
			throw new IOException("Function not implemented");
		}

		@Override
		public void close() throws IOException {
			((InputStream) stream).close();
		}
	}

	public static final class ChunkedStream extends CompatStream {
		private int chunkPos = 0;
		private int chunkLen = 0;

		private final Object stream;

		public ChunkedStream(@NotNull Object stream) {
			this.stream = stream;
			try {
				nextChunk();
			} catch (Exception e) {
				throw new StreamError("Failed to initialize chunked stream", e);
			}
		}

		private void nextChunk() throws IOException {
			String header = new String(readUntil((InputStream) stream, Util.newLineMark, 1024), StandardCharsets.UTF_8);

			int i = header.indexOf(';', 1);
			if (i > 0)
				header = header.substring(0, i);

			try {
				chunkPos = 0;
				chunkLen = Integer.parseInt(header, 16);
			} catch (Exception e) {
				throw new IOException("Invalid chunk length", e);
			}
		}

		@Override
		public int read(@NotNull byte[] buf, int off, int len) throws IOException {
			final int cLen = chunkLen;
			if (cLen <= 0)
				return -1;

			InputStream stream = (InputStream) this.stream;
			if (chunkPos >= cLen) {
				int a = stream.read();
				int b = stream.read();

				if (a != (int) '\r' || b != (int) '\n')
					throw new IOException("Invalid chunk ending token: " + a + ", " + b);

				nextChunk();
			}

			int size = stream.read(buf, off, Math.min(chunkLen - chunkPos, len));
			chunkPos += size;
			return size;
		}

		@Override
		public void close() throws IOException {
			chunkPos = 0;
			chunkLen = 0;
			((InputStream) stream).close();
		}
	}

	public static final class ChunkedOutput extends OutputStream {
		private static final byte[] streamEnd = new byte[]{(byte) '0', (byte) '\r', (byte) '\n', (byte) '\r', (byte) '\n'};

		private final OutputStream stream;

		public ChunkedOutput(@NotNull OutputStream stream) {
			this.stream = stream;
		}

		@Override
		public void write(int b) throws IOException {
			write(new byte[]{(byte) b}, 0, 1);
		}

		@Override
		public void write(@NotNull byte[] b) throws IOException {
			write(b, 0, b.length);
		}

		@Override
		public void write(@NotNull byte[] b, int off, int len) throws IOException {
			byte[] data = Integer.toString(len, 16).getBytes(StandardCharsets.UTF_8);
			stream.write(data, 0, data.length);

			data = Util.newLineMark;
			stream.write(data, 0, 2);
			stream.write(b, off, len);
			stream.write(data, 0, 2);
			stream.flush();
		}

		@Override
		public void flush() throws IOException {
			stream.flush();
		}

		@Override
		public void close() throws IOException {
			stream.write(streamEnd, 0, 5);
			stream.close();
		}
	}

	public abstract static class CompatStream extends InputStream implements Cloneable {
		@Override
		public final int read() throws IOException {
			byte[] buf = new byte[1];
			return read(buf, 0, 1) < 0 ? -1 : buf[0] & 0xff;
		}

		@Override
		public final int read(@NotNull byte[] b) throws IOException {
			return read(b, 0, b.length);
		}

		@Override
		public abstract int read(@NotNull byte[] b, int off, int len) throws IOException;

		@Override
		public abstract void close() throws IOException;

		@Override
		public InputStream clone() {
			try {
				return (InputStream) super.clone();
			} catch (Exception e) {
				return null;
			}
		}

		@Override
		public long transferTo(@NotNull OutputStream out) throws IOException {
			return pipe(this, out);
		}

		@NotNull
		@Override
		public byte[] readNBytes(int len) throws IOException {
			return Streams.readNBytes(this, len);
		}

		@Override
		public int readNBytes(byte[] b, int off, int len) throws IOException {
			int n = 0;

			while (n < len) {
				final int size = read(b, off + n, len - n);
				if (size < 0)
					break;

				n += size;
			}

			return n;
		}

		@NotNull
		@Override
		public byte[] readAllBytes() throws IOException {
			byte[] buffer = new byte[8192];
			byte[] outBuf = new byte[0];

			for (int i = read(buffer, 0, 8192); i >= 0; i = read(buffer, 0, 8192)) {
				int length = outBuf.length;
				byte[] buf = new byte[length + i];
				System.arraycopy(outBuf, 0, buf, 0, length);
				System.arraycopy(buffer, 0, buf, length, i);
				outBuf = buf;
			}

			return outBuf;
		}
	}
}

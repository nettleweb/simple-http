package com.nettleweb.http;

import jdk.internal.vm.annotation.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.zip.*;

public abstract class Body extends Cloneable implements Serializable {
	public static final Body emptyBody = new EmptyBody();

	@NotNull
	public static Body from(@Nullable byte[] data) {
		return data == null ? emptyBody : new BufferBody(data);
	}

	@NotNull
	public static Body from(@Nullable String data) {
		return data == null ? emptyBody : new BufferBody(data.getBytes(StandardCharsets.UTF_8));
	}

	@NotNull
	public static Body from(@Nullable InputStream stream) {
		return stream == null ? emptyBody : new StreamBody(stream, null);
	}

	@NotNull // no longer public because it is unsafe and its usage should be restricted
	static Body from(@Nullable InputStream stream, @Nullable String encoding) {
		return stream == null ? emptyBody : new StreamBody(stream, encoding);
	}

	@Nullable
	public abstract Body body();

	public abstract boolean bodyUsed();

	@NotNull
	public abstract String text();

	@NotNull
	public abstract byte[] buffer();

	@NotNull
	public abstract InputStream stream();

	@ForceInline
	public void pipeTo(@NotNull OutputStream out) {
		pipeTo(out, false);
	}

	public void pipeTo(@NotNull OutputStream out, boolean close) {
		try (InputStream stream = this.stream()) {
			Streams.pipe(stream, out);
			if (close)
				out.close();
		} catch (Exception e) {
			throw new StreamError("Failed to transfer stream", e);
		}
	}

	@NotNull
	@Override
	public Body clone() {
		return (Body) super.clone();
	}

	static abstract class Wrapper extends Body {
		private final Body body;

		Wrapper(@Nullable Body body) {
			this.body = body;
		}

		@Override
		public Body body() {
			return body == null ? null : body.body();
		}

		@Override
		public boolean bodyUsed() {
			return body != null && body.bodyUsed();
		}

		@NotNull
		@Override
		public String text() {
			return body == null ? "" : body.text();
		}

		@NotNull
		@Override
		public byte[] buffer() {
			return body == null ? new byte[0] : body.buffer();
		}

		@NotNull
		@Override
		public InputStream stream() {
			return body == null ? new EmptyStream() : body.stream();
		}

		@Override
		public void pipeTo(@NotNull OutputStream out, boolean close) {
			if (body != null)
				body.pipeTo(out);
			if (close)
				Streams.closeUnchecked(out);
		}
	}

	private static final class EmptyBody extends Body {
		private EmptyBody() {}

		@Override
		public Body body() {
			return null;
		}

		@Override
		public boolean bodyUsed() {
			return false;
		}

		@NotNull
		@Override
		public String text() {
			return "";
		}

		@NotNull
		@Override
		public byte[] buffer() {
			return new byte[0];
		}

		@NotNull
		@Override
		public InputStream stream() {
			return new EmptyStream();
		}

		@Override
		public void pipeTo(@NotNull OutputStream out, boolean close) {
			if (close)
				Streams.closeUnchecked(out);
		}
	}

	private static final class BufferBody extends Body {
		private final byte[] buffer;

		private BufferBody(@NotNull byte[] buffer) {
			this.buffer = buffer;
		}

		@Override
		public Body body() {
			return this;
		}

		@Override
		public boolean bodyUsed() {
			return false;
		}

		@NotNull
		@Override
		public String text() {
			return new String(buffer, StandardCharsets.UTF_8);
		}

		@NotNull
		@Override
		public byte[] buffer() {
			return buffer.clone();
		}

		@NotNull
		@Override
		public InputStream stream() {
			return new ByteArrayInputStream(buffer);
		}

		@Override
		public void pipeTo(@NotNull OutputStream out, boolean close) {
			try {
				out.write(buffer, 0, buffer.length);
				out.flush();
			} catch (Exception e) {
				throw new StreamError("Failed to write buffer", e);
			}

			if (close)
				Streams.closeUnchecked(out);
		}
	}

	private static final class StreamBody extends Body {
		private final StreamWrapper stream;

		@SuppressWarnings("resource")
		private StreamBody(@NotNull Object stream, @Nullable String encoding) {
			if (encoding != null) {
				String[] encodings = encoding.split(",", 10);
				for (int i = encodings.length - 1; i >= 0; i--) {
					switch (encoding = encodings[i]) {
						case "gzip":
							try {
								stream = new GZIPInputStream((InputStream) stream, 4096);
							} catch (Exception e) {
								throw new StreamError("Failed to initialize GZIP stream", e);
							}
							break;
						case "chunked":
							stream = new Streams.ChunkedStream(stream);
							break;
						case "deflate":
							stream = new InflaterInputStream((InputStream) stream, new Inflater(false), 4096);
							break;
						case "compress":
							stream = new Streams.UnsupportedStream(stream);
							break;
						default:
							throw new StreamError("Invalid stream encoding: " + encoding);
					}
				}
			}

			this.stream = new StreamWrapper(stream);
		}

		@Override
		public Body body() {
			return this;
		}

		@Override
		public boolean bodyUsed() {
			return stream.used;
		}

		@NotNull
		@Override
		public String text() {
			StreamWrapper wrapper = this.stream;
			if (wrapper.used)
				throw new IllegalStateException("Body has already been used");

			byte[] data;

			try {
				data = wrapper.readAllBytes();
			} catch (Exception e) {
				throw new StreamError("Failed to read stream data", e);
			}

			wrapper.close();
			return new String(data, StandardCharsets.UTF_8);
		}

		@NotNull
		@Override
		public byte[] buffer() {
			StreamWrapper wrapper = this.stream;
			if (wrapper.used)
				throw new IllegalStateException("Body has already been used");

			byte[] data;

			try {
				data = wrapper.readAllBytes();
			} catch (Exception e) {
				throw new StreamError("Failed to read stream data", e);
			}

			wrapper.close();
			return data;
		}

		@NotNull
		@Override
		public InputStream stream() {
			if (stream.used)
				throw new IllegalStateException("Body has already been used.");

			return stream;
		}

		@NotNull
		@Override
		public Body clone() {
			Body value = super.clone();
			try {
				Field field = StreamBody.class.getDeclaredField("stream");
				field.setAccessible(true);
				field.set(value, stream.clone());
			} catch (Exception e) {
				throw new RuntimeException("Failed to clone stream", e);
			}
			return value;
		}
	}

	private static final class StreamWrapper extends Streams.CompatStream {
		private boolean used;
		private final Object stream;

		private StreamWrapper(@NotNull Object stream) {
			this.stream = stream;
		}

		@Override
		public int read(@NotNull byte[] b, int off, int len) throws IOException {
			used = true;
			return ((InputStream) stream).read(b, off, len);
		}

		@Override
		public void close() {
			used = true;
			Streams.closeUnchecked((InputStream) stream);
		}
	}
}
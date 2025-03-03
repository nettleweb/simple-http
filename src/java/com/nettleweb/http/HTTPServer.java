package com.nettleweb.http;

import org.jetbrains.annotations.*;

import javax.net.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.charset.*;
import java.util.concurrent.*;

public class HTTPServer implements Runnable, Flushable, Closeable {
	private static final HTTPResponse msg500 = new HTTPResponse(500, "", new Headers(
			"Connection: close",
			"Content-Type: text/plain"
	), Body.from("500 Internal Server Error"));

	@NotNull
	public final String host;
	public final int port;
	public final int backlog;

	private int socketTimeout = 10000;
	private int maxHeaderSize = 65536;

	@NotNull
	private Executor executor;
	@NotNull
	private HTTPLogger logger;
	@NotNull
	private HTTPHandler handler;
	@Nullable
	private ServerSocket socket;
	@NotNull
	private ServerSocketFactory factory;

	{
		logger = DefaultLogger.instance;
		handler = DefaultHandler.instance;
		factory = ServerSocketFactory.getDefault();
	}

	public HTTPServer(int port) {
		this(null, port);
	}

	public HTTPServer(@Nullable String host, int port) {
		this(host, port, 255);
	}

	public HTTPServer(@Nullable String host, int port, int backlog) {
		this(host, port, backlog, null);
	}

	public HTTPServer(@Nullable String host, int port, int backlog, @Nullable Executor executor) {
		if (port < 0 || port > 65535)
			throw new IllegalArgumentException("Invalid port: " + port);

		this.host = (host == null || host.isEmpty()) ? "0.0.0.0" : host;
		this.port = port;
		this.backlog = backlog;
		this.executor = executor == null ? DefaultExecutor.instance : executor;
	}

	public void setLogger(@Nullable HTTPLogger logger) {
		this.logger = logger == null ? DefaultLogger.instance : logger;
	}

	public void setHandler(@Nullable HTTPHandler handler) {
		this.handler = handler == null ? DefaultHandler.instance : handler;
	}

	public void setExecutor(@Nullable Executor executor) {
		this.executor = executor == null ? DefaultExecutor.instance : executor;
	}

	public void setSocketFactory(@Nullable ServerSocketFactory factory) {
		this.factory = factory == null ? ServerSocketFactory.getDefault() : factory;
	}

	public void setSocketTimeout(int socketTimeout) {
		this.socketTimeout = socketTimeout;
	}

	public void setMaxHeaderSize(int maxHeaderSize) {
		this.maxHeaderSize = maxHeaderSize;
	}

	@Override
	public void run() {
		ServerSocket server = this.socket;
		if (server == null || !server.isBound())
			throw new IllegalStateException("Invalid server socket state.");

		while (!server.isClosed()) {
			try {
				Method m = Thread.class.getDeclaredMethod("sleep", long.class);
				m.setAccessible(true);
				m.invoke(null, 30L);
			} catch (Exception e) {
				break;
			}

			try {
				Socket socket = server.accept();
				socket.setKeepAlive(false);
				socket.setTcpNoDelay(true);
				socket.setReuseAddress(true);
				socket.setSendBufferSize(65536);
				socket.setReceiveBufferSize(8192);

				if (socketTimeout > 0)
					socket.setSoTimeout(socketTimeout);

				executor.execute(() -> {
					try (InputStream is = socket.getInputStream(); OutputStream os = socket.getOutputStream()) {
						byte[] data = Streams.readUntil(is, Util.headerEndMark, maxHeaderSize);
						HTTPRequest request = Util.parseRawRequest(new String(data, StandardCharsets.UTF_8), is);
						HTTPResponse response = msg500;

						try {
							response = handler.handleRequest(request);
							if (response == null) {
								response = msg500;
								logger.warn("HTTP handler returned null response");
							}
						} catch (Exception e) {
							logger.error("HTTP handler returned error: ", e);
						}

						socket.shutdownInput();
						StringBuilder builder = new StringBuilder("HTTP/1.1 ").append(response.status);

						{
							String msg = response.message;
							if (!msg.isEmpty())
								builder.append(' ').append(msg);
						}

						Body body = response.body();
						Headers headers = response.headers;
						boolean hasBody = body != null && !"HEAD".equals(request.method);
						boolean chunked = hasBody && !headers.has("content-length");

						headers.set("date", DateFormatter.utc());
						headers.set("server", "NettleWeb v0.1.0");
						if (chunked)
							headers.set("transfer-encoding", "chunked");

						headers.toString(builder.append("\r\n")).append("\r\n");
						data = builder.toString().getBytes(StandardCharsets.UTF_8);
						os.write(data, 0, data.length);
						os.flush();

						if (hasBody)
							body.pipeTo(chunked ? new Streams.ChunkedOutput(os) : os, true);
					} catch (Exception e) {
						logger.error("Error while handling HTTP request: ", e);
					}
				});
			} catch (Exception e) {
				logger.error("Failed to setup TCP connection: ", e);
			}
		}
	}

	public void start() {
		try {
			ServerSocket socket = factory.createServerSocket();
			socket.setReuseAddress(true);
			socket.setReceiveBufferSize(8192);
			(this.socket = socket).bind(new InetSocketAddress(host, port), backlog);
		} catch (Exception e) {
			throw new ServerError("Failed to start HTTP server: ", e);
		}

		executor.execute(this);
	}

	@Override
	public void flush() {
	}

	@Override
	public void close() {
		if (socket != null) {
			try {
				socket.close();
			} catch (Exception e) {
				// ignore
			}
		}
	}
}
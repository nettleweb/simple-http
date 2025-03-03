package com.nettleweb.http;

import org.jetbrains.annotations.*;

import javax.net.*;
import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;

public class HTTPClient {
	private int timeout = 5000;
	private int socketTimeout = 15000;
	private int maxHeaderSize = 65536;

	@NotNull
	private SocketFactory factory = SocketFactory.getDefault();

	public HTTPClient() {
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public void setSocketFactory(@Nullable SocketFactory factory) {
		this.factory = factory == null ? SocketFactory.getDefault() : factory;
	}

	public void setSocketTimeout(int socketTimeout) {
		this.socketTimeout = socketTimeout;
	}

	public void setMaxHeaderSize(int maxHeaderSize) {
		this.maxHeaderSize = maxHeaderSize;
	}

	@Nullable
	public HTTPResponse opt(@Nullable HTTPRequest request) {
		if (request == null)
			return null;

		try {
			return fetch(request);
		} catch (Exception e) {
			return null;
		}
	}

	@NotNull
	public HTTPResponse fetch(@NotNull HTTPRequest request) {
		URI uri = Util.optURL(request.url);
		String scheme;

		if (uri == null || (scheme = uri.getScheme()) == null)
			throw new IllegalArgumentException("Invalid request URL: " + uri);

		String host = uri.getHost();
		int port = uri.getPort();

		switch (scheme) {
			case "data":
				return fetchDataURL(uri.getRawSchemeSpecificPart());
			case "http":
				if (port < 0 || port > 65535)
					port = 80;
				break;
			case "https":
				if (port < 0 || port > 65535)
					port = 443;
				break;
			default:
				throw new IllegalArgumentException("Unsupported URL protocol: " + scheme);
		}

		try {
			Socket socket = factory.createSocket();
			socket.connect(new InetSocketAddress(host, port), timeout);
			socket.setSoTimeout(socketTimeout);
			socket.setKeepAlive(false);
			socket.setTcpNoDelay(false);
			socket.setReuseAddress(true);
			socket.setSendBufferSize(65536);
			socket.setReceiveBufferSize(65536);

			InputStream in = socket.getInputStream();
			OutputStream out = socket.getOutputStream();

			{
				StringBuilder builder = new StringBuilder(request.method).append(' ');

				{
					String path = uri.getRawPath();
					if (path == null || path.isEmpty())
						builder.append('/');
					else
						builder.append(path);
				}

				{
					String query = uri.getRawQuery();
					if (query != null)
						builder.append('?').append(query);
				}
				{
					String fragment = uri.getRawFragment();
					if (fragment != null)
						builder.append('#').append(fragment);
				}

				Body body = request.body();
				Headers headers = request.headers;
				boolean chunked = body != null && !headers.has("content-length");

				if (port == 80 || port == 443)
					headers.set("host", host);
				else
					headers.set("host", host + ":" + port);
				if (chunked)
					headers.set("transfer-encoding", "chunked");

				headers.toString(builder.append(" HTTP/1.1\r\n")).append("\r\n");

				{
					byte[] data = builder.toString().getBytes(StandardCharsets.UTF_8);
					out.write(data, 0, data.length);
					out.flush();
				}

				if (body != null)
					body.pipeTo(chunked ? new Streams.ChunkedOutput(out) : out, false);
			}

			return Util.parseRawResponse(new String(Streams.readUntil(in, Util.headerEndMark, maxHeaderSize), StandardCharsets.UTF_8), in);
		} catch (Exception e) {
			throw new NetworkError("Failed to connect to origin server", e);
		}
	}

	private static HTTPResponse fetchDataURL(String path) {
		int sep = path.indexOf(',', 1);
		if (sep < 1)
			throw new NetworkError("Invalid URL");

		String head = path.substring(0, sep);
		String data = path.substring(sep + 1);

		try {
			data = URLDecoder.decode(data.replace("+", "%2B"), "UTF-8").replace("%2B", "+");
		} catch (UnsupportedEncodingException e) {
			throw new Error(e);
		}

		if (head.endsWith(";base64"))
			return new HTTPResponse(200, "", new Headers("Content-Type: " + head.substring(0, head.length() - 7)), Body.from(Base64.getDecoder().decode(data)));
		else
			return new HTTPResponse(200, "", new Headers("Content-Type: " + head), Body.from(data));
	}
}
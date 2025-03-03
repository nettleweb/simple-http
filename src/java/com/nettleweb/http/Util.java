package com.nettleweb.http;

import org.jetbrains.annotations.*;

import java.io.*;
import java.net.*;
import java.util.*;

final class Util {
	public static final byte[] newLineMark = new byte[]{(byte) '\r', (byte) '\n'};
	public static final byte[] headerEndMark = new byte[]{(byte) '\r', (byte) '\n', (byte) '\r', (byte) '\n'};

	private Util() {}

	public static URI optURL(String url) {
		if (url == null)
			return null;

		try {
			return new URI(url);
		} catch (Exception e) {
			return null;
		}
	}

	@NotNull
	public static HTTPRequest parseRawRequest(String message, InputStream stream) {
		if ((message = message.trim()).isEmpty())
			throw new ProtocolError("Empty message");

		String[] lines = message.split("(\\r\\n|\\n)");
		if (lines.length < 2)
			throw new ProtocolError("Missing required values");

		String[] parts = parseRequestHead(lines[0]);
		if (parts == null || !parts[2].equals("HTTP/1.1"))
			throw new ProtocolError("Invalid message header");

		String rawPath = parts[1].trim();
		if (rawPath.charAt(0) != '/')
			throw new ProtocolError("Invalid request path");

		Headers headers = new Headers(Arrays.copyOfRange(lines, 1, lines.length));

		String host = headers.get("host");
		if (host == null || host.isEmpty())
			throw new ProtocolError("Invalid host header");

		return new HTTPRequest("http://" + host + rawPath, parts[0].trim(), headers, Body.from(stream, headers.get("transfer-encoding")));
	}

	@NotNull
	public static HTTPResponse parseRawResponse(String message, InputStream stream) {
		if ((message = message.trim()).isEmpty())
			throw new ProtocolError("Empty message");

		String[] lines = message.split("(\\r\\n|\\n)");
		if (lines.length < 2)
			throw new NetworkError("missing required headers");

		int status;

		String[] parts = parseResponseHead(lines[0]);
		if (parts == null)
			throw new NetworkError("Missing required info");

		try {
			status = Integer.parseInt(parts[1], 10);
		} catch (Exception e) {
			throw new ProtocolError(e);
		}

		switch (parts[0]) {
			case "HTTP/1.0":
			case "HTTP/1.1":
				break;
			default:
				throw new ProtocolError("Unsupported protocol: " + parts[0]);
		}

		message = parts.length >= 3 ? parts[2] : "";
		Headers headers = new Headers(Arrays.copyOfRange(lines, 1, lines.length));

		{
			String length = headers.get("content-length");
			if (length != null) {
				byte[] buffer;

				try {
					buffer = Streams.readNBytes(stream, Integer.parseInt(length, 10));
					stream.close();
				} catch (Exception e) {
					throw new StreamError("Failed to read response body", e);
				}

				return new HTTPResponse(status, message, headers, Body.from(buffer));
			}
		}

		return new HTTPResponse(status, message, headers, Body.from(stream, headers.get("transfer-encoding")));
	}

	private static String[] parseRequestHead(String head) {
		int i = head.indexOf(' ', 1);
		if (i < 1)
			return null;

		int i2 = head.lastIndexOf(' ');
		if (i2 < 1)
			return null;

		return new String[]{
				head.substring(0, i),
				head.substring(i + 1, i2),
				head.substring(i2 + 1)
		};
	}

	private static String[] parseResponseHead(String head) {
		int i = head.indexOf(' ', 1);
		if (i < 1)
			return null;

		int i2 = head.indexOf(' ', i + 1);
		if (i2 < 1)
			return null;

		return new String[]{
				head.substring(0, i),
				head.substring(i + 1, i2),
				i2 < head.length() - 1 ? head.substring(i2 + 1) : ""
		};
	}
}
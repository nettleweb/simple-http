package com.nettleweb.http;

import org.jetbrains.annotations.*;

import java.io.*;

public final class HTTPResponse extends Body.Wrapper {
	@Serial
	private static final long serialVersionUID = 7692952938828246186L;

	public int status;
	@NotNull
	public String message;
	@NotNull
	public final Headers headers;

	public HTTPResponse() {
		this(200);
	}

	public HTTPResponse(int status) {
		this(200, null);
	}

	public HTTPResponse(int status, @Nullable String message) {
		this(status, message, null, null);
	}

	public HTTPResponse(int status, @Nullable String message, @Nullable Headers headers) {
		this(status, message, headers, null);
	}

	public HTTPResponse(int status, @Nullable String message, @Nullable Headers headers, @Nullable Body body) {
		super(body);
		this.status = status;
		this.message = message == null ? "" : message;
		this.headers = headers == null ? new Headers() : headers;
	}

	public boolean ok() {
		return status >= 200 && status < 300;
	}

	@NotNull
	@Override
	public HTTPResponse clone() {
		return (HTTPResponse) super.clone();
	}
}
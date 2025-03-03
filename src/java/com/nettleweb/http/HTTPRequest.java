package com.nettleweb.http;

import org.jetbrains.annotations.*;

import java.io.*;

public final class HTTPRequest extends Body.Wrapper implements Serializable {
	@Serial
	private static final long serialVersionUID = 1415296166320098175L;

	@NotNull
	public String url;
	@NotNull
	public String method;
	@NotNull
	public final Headers headers;

	public HTTPRequest(@NotNull String url) {
		this(url, null);
	}

	public HTTPRequest(@NotNull String url, @Nullable String method) {
		this(url, method, null);
	}

	public HTTPRequest(@NotNull String url, @Nullable String method, @Nullable Headers headers) {
		this(url, method, headers, null);
	}

	public HTTPRequest(@NotNull String url, @Nullable String method, @Nullable Headers headers, @Nullable Body body) {
		super(body);
		this.url = url;
		this.method = method == null ? "GET" : method;
		this.headers = headers == null ? new Headers() : headers;
	}

	@NotNull
	@Override
	public HTTPRequest clone() {
		return (HTTPRequest) super.clone();
	}
}
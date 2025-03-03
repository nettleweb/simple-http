package com.nettleweb.http;

import org.jetbrains.annotations.*;

final class DefaultHandler implements HTTPHandler {
	public static final DefaultHandler instance = new DefaultHandler();

	private DefaultHandler() {
	}

	@Override
	public HTTPResponse handleRequest(@NotNull HTTPRequest request) {
		return new HTTPResponse(404, "", new Headers("Content-Type: text/plain"), Body.from("404 Not Found"));
	}
}
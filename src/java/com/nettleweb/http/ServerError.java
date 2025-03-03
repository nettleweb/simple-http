package com.nettleweb.http;

import java.io.*;

public class ServerError extends NetworkError {
	@Serial
	private static final long serialVersionUID = -5426910330762352387L;

	public ServerError() {
	}

	public ServerError(String message) {
		super(message);
	}

	public ServerError(Throwable cause) {
		super(cause);
	}

	public ServerError(String message, Throwable cause) {
		super(message, cause);
	}
}
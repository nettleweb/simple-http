package com.nettleweb.http;

import java.io.*;

public class NetworkError extends RuntimeException {
	@Serial
	private static final long serialVersionUID = 1886271530813154173L;

	public NetworkError() {
	}

	public NetworkError(String message) {
		super(message);
	}

	public NetworkError(Throwable cause) {
		super(cause);
	}

	public NetworkError(String message, Throwable cause) {
		super(message, cause);
	}
}
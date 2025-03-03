package com.nettleweb.http;

import java.io.*;

public class StreamError extends NetworkError {
	@Serial
	private static final long serialVersionUID = -6386407974658053486L;

	public StreamError() {
	}

	public StreamError(String message) {
		super(message);
	}

	public StreamError(Throwable cause) {
		super(cause);
	}

	public StreamError(String message, Throwable cause) {
		super(message, cause);
	}
}
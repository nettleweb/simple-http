package com.nettleweb.http;

public class ProtocolError extends NetworkError {
	public ProtocolError() {
	}

	public ProtocolError(String message) {
		super(message);
	}

	public ProtocolError(Throwable cause) {
		super(cause);
	}

	public ProtocolError(String message, Throwable cause) {
		super(message, cause);
	}
}
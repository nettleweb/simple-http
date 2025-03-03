package com.nettleweb.http;

import org.jetbrains.annotations.*;

import java.io.*;
import java.nio.charset.*;

final class DefaultLogger extends HTTPLogger {
	public static final DefaultLogger instance = new DefaultLogger();

	private static final byte[] nullBytes = new byte[]{110, 117, 108, 108};
	private static final byte[] newLine = new byte[]{(byte) '\n'};

	private DefaultLogger() {}

	@Override
	public void println(@Nullable String data) {
		try {
			OutputStream out = System.out;

			if (data != null)
				out.write(data.getBytes(StandardCharsets.UTF_8));
			else
				out.write(nullBytes);

			out.write(newLine);
			out.flush();
		} catch (Exception e) {
			// ignore
		}
	}

	@Override
	public void printErr(@Nullable String data) {
		try {
			OutputStream out = System.err;

			if (data != null)
				out.write(data.getBytes(StandardCharsets.UTF_8));
			else
				out.write(nullBytes);

			out.write(newLine);
			out.flush();
		} catch (Exception e) {
			// ignore
		}
	}
}
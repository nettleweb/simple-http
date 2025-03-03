package com.nettleweb.http;

import jdk.internal.vm.annotation.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.nio.charset.*;

public abstract class HTTPLogger {
	@NotNull
	public static HTTPLogger getDefault() {
		return DefaultLogger.instance;
	}

	private static char[] getChars(CharSequence seq) {
		final int length = seq.length();

		char[] data = new char[length];
		for (int i = 0; i < length; i++)
			data[i] = seq.charAt(i);

		return data;
	}

	private static String objectToString(Object obj) {
		if (obj == null)
			return "null";
		if (obj instanceof String)
			return (String) obj;
		if (obj instanceof CharSequence)
			return new String(getChars((CharSequence) obj));

		if (obj instanceof Throwable) {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			((Throwable) obj).printStackTrace(new PrintStream(stream));
			return new String(stream.toByteArray(), StandardCharsets.UTF_8);
		}

		return obj.toString();
	}

	private static StringBuilder log() {
		return new StringBuilder("[HTTP LOG] ")
				.append(DateFormatter.iso())
				.append(" ");
	}

	private static StringBuilder warn() {
		return new StringBuilder("[HTTP WARN] ")
				.append(DateFormatter.iso())
				.append(" ");
	}

	private static StringBuilder error() {
		return new StringBuilder("[HTTP ERROR] ")
				.append(DateFormatter.iso())
				.append(" ");
	}

	public void log(Object... data) {
		StringBuilder builder = log();
		for (Object obj : data)
			builder.append(objectToString(obj).trim()).append(" ");

		println(builder.substring(0, builder.length() - 1));
	}

	@ForceInline
	public void log(@Nullable String data) {
		println(log().append(data).toString());
	}

	public void warn(Object... data) {
		StringBuilder builder = warn();
		for (Object obj : data)
			builder.append(objectToString(obj).trim()).append(" ");

		warn(builder.substring(0, builder.length() - 1));
	}

	@ForceInline
	public void warn(@Nullable String data) {
		println(warn().append(data).toString());
	}

	public void error(Object... data) {
		StringBuilder builder = error();
		for (Object obj : data)
			builder.append(objectToString(obj).trim()).append(" ");

		error(builder.substring(0, builder.length() - 1));
	}

	@ForceInline
	public void error(@Nullable String data) {
		printErr(error().append(data).toString());
	}

	@IntrinsicCandidate
	public abstract void println(@Nullable String data);

	@IntrinsicCandidate
	public abstract void printErr(@Nullable String data);
}
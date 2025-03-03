package com.nettleweb.http;

import jdk.internal.vm.annotation.*;

import java.text.*;
import java.util.*;

final class DateFormatter {
	private static final SimpleDateFormat utcDate = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
	private static final SimpleDateFormat isoDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS zzz", Locale.ROOT);

	static {
		utcDate.setTimeZone(TimeZone.getTimeZone("GMT"));
		isoDate.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	private DateFormatter() {}

	@IntrinsicCandidate
	public static String iso() {
		return isoDate.format(new Date(System.currentTimeMillis()));
	}

	@IntrinsicCandidate
	public static String utc() {
		return utcDate.format(new Date(System.currentTimeMillis()));
	}
}
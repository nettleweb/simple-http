package com.nettleweb.http;

import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;

public final class Header extends Cloneable implements Map.Entry<String, String>, Serializable, Comparable<Header> {
	@Serial
	private static final long serialVersionUID = -84888603426238694L;

	private final String k;
	private String v;

	public Header(@NotNull Header header) {
		this.k = header.k;
		this.v = header.v;
	}

	public Header(@NotNull String k, @NotNull String v) {
		this.k = k.trim().toLowerCase(Locale.ROOT);
		this.v = v.trim();
	}

	@NotNull
	@Override
	public String getKey() {
		return k;
	}

	@NotNull
	@Override
	public String getValue() {
		return v;
	}

	@Override
	public String setValue(@NotNull String v) {
		String ov = this.v;
		this.v = v;
		return ov;
	}

	@Override
	public Header clone() {
		return (Header) super.clone();
	}

	@Override
	public int hashCode() {
		return k.hashCode() ^ v.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || (obj instanceof Header && k.equals(((Header) obj).k) && v.equals(((Header) obj).v));
	}

	@Override
	public int compareTo(@NotNull Header obj) {
		int i = k.compareTo(obj.k);
		return i == 0 ? v.compareTo(obj.v) : i;
	}

	@Override
	public String toString() {
		return k + "=" + v; // match the behavior of HashMap
	}
}
package com.nettleweb.http;

import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;

public final class Headers extends Cloneable implements Serializable {
	@Serial
	private static final long serialVersionUID = 2716091711085762002L;

	@NotNull
	private Header[] headers;

	private static Header[] merge(Header[] one, Header[] two) {
		int len1 = one.length;
		int len2 = two.length;

		Header[] merged = new Header[len1 + len2];
		System.arraycopy(one, 0, merged, 0, len1);
		System.arraycopy(two, 0, merged, len1, len2);

		return merged;
	}

	private static String[] append(String[] base, String value) {
		int length = base.length;
		String[] n = new String[length + 1];
		System.arraycopy(base, 0, n, 0, length);
		n[length] = value;
		return n;
	}

	private static Header[] append(Header[] base, Header value) {
		int length = base.length;
		Header[] n = new Header[length + 1];
		System.arraycopy(base, 0, n, 0, length);
		n[length] = value;
		return n;
	}

	private static Header[] remove(Header[] base, int i) {
		int length = base.length;
		Header[] n = new Header[length - 1];
		System.arraycopy(base, 0, n, 0, i);

		if (i != length)
			System.arraycopy(base, i + 1, n, i, length - i - 1);

		return n;
	}

	private static Header[] deepClone(Header[] headers) {
		Header[] entries = headers.clone();
		for (int i = 0; i < entries.length; i++)
			entries[i] = new Header(headers[i]);

		return entries;
	}

	public Headers(@NotNull Headers headers) {
		this.headers = deepClone(headers.headers);
	}

	public Headers(@NotNull Map<String, ?> headers) {
		List<Header> entries = new ArrayList<>(headers.size());

		for (Map.Entry<String, ?> e : headers.entrySet()) {
			String k = e.getKey().toLowerCase(Locale.ROOT);
			Object v = e.getValue();

			if (v instanceof String) {
				entries.add(new Header(k, (String) v));
				continue;
			}

			if (v instanceof String[]) {
				for (String item : (String[]) v)
					entries.add(new Header(k, item));

				continue;
			}

			if (v instanceof Collection) {
				for (Object item : (Collection<?>) v)
					entries.add(new Header(k, item.toString()));

				continue;
			}

			entries.add(new Header(k, v.toString()));
		}

		this.headers = entries.toArray(new Header[0]);
	}

	public Headers(@NotNull String... headers) {
		int length = headers.length;
		Header[] entries = new Header[length];

		for (int i = 0; i < length; i++) {
			String e = headers[i].replace('=', ':');

			int p = e.indexOf(':', 1);
			if (p <= 0)
				throw new IllegalArgumentException("Invalid header entry: " + e);

			entries[i] = new Header(e.substring(0, p), e.substring(p + 1));
		}

		this.headers = entries;
	}

	public Headers(@NotNull Iterable<String> headers) {
		List<Header> entries = new ArrayList<>();
		for (String e : headers) {
			int i = (e = e.replace('=', ':')).indexOf(':', 1);
			if (i <= 0)
				throw new IllegalArgumentException("Invalid header entry: " + e);

			entries.add(new Header(e.substring(0, i), e.substring(i + 1)));
		}

		this.headers = entries.toArray(new Header[0]);
	}

	public boolean has(@Nullable String k) {
		if (k != null) {
			k = k.trim().toLowerCase(Locale.ROOT);
			for (Header e : headers) {
				if (e.getKey().equals(k))
					return true;
			}
		}
		return false;
	}

	@Nullable
	public String get(@Nullable String k) {
		if (k != null) {
			k = k.trim().toLowerCase(Locale.ROOT);
			for (Header e : headers)
				if (e.getKey().equals(k))
					return e.getValue();
		}
		return null;
	}

	public void add(@NotNull String k, @NotNull String v) {
		headers = append(headers, new Header(k, v));
	}

	public Header[] set(@NotNull String k, @NotNull String v) {
		Header[] removed = remove(k);
		headers = append(headers, new Header(k, v));
		return removed;
	}

	public int size() {
		return headers.length;
	}

	public void sort() {
		Arrays.sort(headers);
	}

	public void clear() {
		headers = new Header[0];
	}

	@NotNull
	public String[] getAll(@Nullable String k) {
		String[] result = new String[0];
		if (k != null) {
			k = k.trim().toLowerCase(Locale.ROOT);

			for (Header e : headers) {
				if (e.getKey().equals(k))
					result = append(result, e.getValue());
			}
		}
		return result;
	}

	public void addAll(@NotNull Headers h) {
		headers = merge(headers, h.headers);
	}

	public Header[] remove(@Nullable String k) {
		Header[] removed = new Header[0];
		if (k != null) {
			k = k.trim().toLowerCase(Locale.ROOT);
			Header[] entries = headers;

			for (int i = 0; i < entries.length; i++) {
				Header e = entries[i];
				if (e.getKey().equals(k)) {
					headers = entries = remove(entries, i);
					removed = append(removed, e);
				}
			}
		}
		return removed;
	}

	@NotNull
	public String[] keys() {
		Header[] entries = headers;
		int length = entries.length;

		String[] keys = new String[length];
		for (int i = 0; i < length; i++)
			keys[i] = entries[i].getKey();

		return keys;
	}

	@NotNull
	public String[] values() {
		Header[] entries = headers;
		int length = entries.length;

		String[] values = new String[length];
		for (int i = 0; i < length; i++)
			values[i] = entries[i].getValue();

		return values;
	}

	@NotNull
	public Header[] entries() {
		return headers.clone();
	}

	@Override
	public Headers clone() {
		return (Headers) super.clone();
	}

	StringBuilder toString(StringBuilder builder) {
		sort();

		for (Header header : headers) {
			builder.append(header.getKey())
					.append(": ")
					.append(header.getValue())
					.append("\r\n");
		}
		return builder;
	}

	@Override
	public String toString() {
		return toString(new StringBuilder()).toString();
	}
}
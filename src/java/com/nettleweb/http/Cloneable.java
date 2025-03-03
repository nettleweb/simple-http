package com.nettleweb.http;

abstract class Cloneable implements java.lang.Cloneable {
	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (Exception e) {
			throw new Error("Failed to clone object: ", e);
		}
	}
}
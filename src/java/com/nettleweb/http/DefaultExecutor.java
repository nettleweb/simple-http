package com.nettleweb.http;

import org.jetbrains.annotations.*;

import java.util.concurrent.*;

final class DefaultExecutor implements Executor {
	public static final DefaultExecutor instance = new DefaultExecutor();

	private DefaultExecutor() {}

	@Override
	public void execute(@NotNull Runnable r) {
		r.run();
	}
}
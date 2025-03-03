package com.nettleweb.http;

import org.jetbrains.annotations.*;

public interface HTTPHandler {
	HTTPResponse handleRequest(@NotNull HTTPRequest request) throws Exception;
}
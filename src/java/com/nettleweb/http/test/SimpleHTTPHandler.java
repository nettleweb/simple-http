package com.nettleweb.http.test;

import com.nettleweb.http.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.net.*;
import java.nio.file.*;

public final class SimpleHTTPHandler implements HTTPHandler {
	private final String baseDir;

	public SimpleHTTPHandler(String baseDir) {
		this.baseDir = baseDir;
	}

	private static String getFilePath(String base, String path) {
		File file = new File(base, path);
		if (file.exists()) {
			if (file.isDirectory())
				return (file = new File(file, "index.html")).exists() ? file.getAbsolutePath() : null;
			else
				return file.getAbsolutePath();
		}
		return null;
	}

	private static String getMimeType(String path) {
		int i = path.lastIndexOf('/'); // get file name first to prevent dot in parent directory being used
		if (i >= 0)
			path = path.substring(i);

		if ((i = path.lastIndexOf('.')) >= 0) // get file extension
			path = path.substring(i);
		else // file without no extension
			return "application/octet-stream";

		switch (path) {
			// image
			case ".png":
				return "image/png";
			case ".apng":
				return "image/apng";
			case ".avif":
				return "image/avif";
			case ".bmp":
				return "image/bmp";
			case ".gif":
				return "image/gif";
			case ".ico":
				return "image/x-icon";
			case ".jpg":
			case ".jpeg":
				return "image/jpeg";
			case ".svg":
				return "image/svg+xml";
			case ".tif":
			case ".tiff":
				return "image/tiff";
			case ".webp":
				return "image/webp";

			// audio
			case ".aac":
				return "audio/aac";
			case ".flac":
				return "audio/flac";
			case ".mid":
			case ".midi":
				return "audio/midi";
			case ".mp3":
				return "audio/mpeg";
			case ".oga":
			case ".ogg":
			case ".opus":
				return "audio/ogg";
			case ".wav":
				return "audio/wav";
			case ".weba":
				return "audio/webm";

			// video
			case ".avi":
				return "video/x-msvideo";
			case ".mp4":
				return "video/mp4";
			case ".mpeg":
				return "video/mpeg";
			case ".ogv":
				return "video/ogg";
			case ".ts":
				return "video/mp2t";
			case ".webm":
				return "video/webm";

			// fonts
			case ".otf":
				return "font/otf";
			case ".ttf":
				return "font/ttf";
			case ".woff":
				return "font/woff";
			case ".woff2":
				return "font/woff2";

			// misc
			case ".js":
			case ".cjs":
			case ".mjs":
				return "text/javascript";
			case ".css":
				return "text/css";
			case ".csv":
				return "text/csv";
			case ".txt":
				return "text/plain";
			case ".pdf":
				return "application/pdf";
			case ".rtf":
				return "application/rtf";
			case ".xml":
				return "application/xml";
			case ".json":
				return "application/json";
			case ".htm":
			case ".html":
				return "text/html";
			case ".xht":
			case ".xhtml":
				return "application/xhtml+xml";

			// fallback
			default:
				return "application/octet-stream";
		}
	}

	@Override
	public HTTPResponse handleRequest(@NotNull HTTPRequest request) throws Exception {
		String method = request.method;
		switch (method) {
			case "GET":
			case "HEAD":
				break;
			case "OPTIONS":
				return new HTTPResponse(200, null, new Headers("Allow: GET, HEAD, OPTIONS"), null);
			default:
				return new HTTPResponse(200, null, new Headers(
						"Allow: GET, HEAD, OPTIONS",
						"Content-Type: text/plain"
				), Body.from("405 Method Not Allowed"));
		}

		URI uri = new URI(request.url);
		String path = URLDecoder.decode(uri.getRawPath(), "UTF-8");
		String file = getFilePath(baseDir, path);

		if (file != null) {
			if (path.charAt(path.length() - 1) != '/' && file.endsWith("index.html") && !path.endsWith("index.html")) {
				return new HTTPResponse(301, null, new Headers(
						"Content-Type: text/plain",
						"Location: " + path + "/" + uri.getRawQuery()
				), Body.from("301 Moved Permanently"));
			}

			long size = new File(file).length();
			String mime = getMimeType(file);

			if (method.equals("HEAD")) {
				return new HTTPResponse(200, "", new Headers(
						"Content-Type: " + mime,
						"Content-Length: " + size
				));
			} else {
				return new HTTPResponse(200, "", new Headers(
						"Content-Type: " + mime,
						"Content-Length: " + size
				), Body.from(Files.newInputStream(Paths.get(file), StandardOpenOption.READ)));
			}
		} else return new HTTPResponse(404, "", new Headers(
				"Content-Type: text/plain"
		), Body.from("404 Not Found"));
	}
}

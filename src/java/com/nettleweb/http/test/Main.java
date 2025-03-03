package com.nettleweb.http.test;

import com.nettleweb.http.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.concurrent.*;

public final class Main {
	private Main() {}

	@TestOnly
	public static void main(String[] args) {
		final HTTPLogger logger = HTTPLogger.getDefault();

		int port = 8080;
		int parse = 0;
		int threads = 4;
		int backlog = 255;

		String host = "0.0.0.0";
		String baseDir = new File(".").getAbsolutePath();

		for (String arg: args) {
			if (arg.length() > 1 && arg.charAt(0) == '-') {
				switch (arg = arg.charAt(1) == '-' ? arg.substring(2) : arg.substring(1)) {
					case "host":
						parse = 1;
						break;
					case "port":
						parse = 2;
						break;
					case "threads":
						parse = 3;
						break;
					case "backlog":
						parse = 4;
						break;
					case "help":
						logger.println("Usage: simple-http [OPTION...]\n");
						logger.println("\t--host <name>\t\tStart the HTTP server with the specified host.");
						logger.println("\t--port <port>\t\tStart the HTTP server with the specified port.");
						logger.println("\t--help\t\t\t\tShow this help message and exit.");
						logger.println("\t--version\t\t\t Show version information and exit.");
						return;
					case "version":
						logger.println("v0.1.0");
						return;
					default:
						logger.printErr("Error: Invalid option: --" + arg);
						logger.printErr("Try '--help' for more information.");
						System.exit(1);
						return;
				}
			} else {
				switch (parse) {
					case 1:
						host = arg;
						parse = 0;
						break;
					case 2:
						try {
							port = Integer.parseInt(arg, 10);
							if (port < 0 || port > 65535) {
								logger.printErr("Error: Port must be between 0 and 65535.");
								System.exit(1);
								return;
							}
						} catch (Exception e) {
							logger.printErr("Error: Invalid port value: " + arg);
							System.exit(1);
							return;
						}
						parse = 0;
						break;
					case 3:
						try {
							threads = Integer.parseInt(arg, 10);
							if (threads < 0 || threads > 1024) {
								logger.printErr("Error: Threads must be between 0 and 1024.");
								System.exit(1);
								return;
							}
						} catch (Exception e) {
							logger.println("Error: Invalid threads value: " + arg);
							System.exit(1);
							return;
						}
						parse = 0;
						break;
					case 4:
						try {
							backlog = Integer.parseInt(arg, 10);
						} catch (Exception e) {
							logger.println("Error: Invalid backlog value: " + arg);
							System.exit(1);
							return;
						}
						parse = 0;
						break;
					default:
						logger.printErr("Error: Invalid arguments.");
						logger.printErr("Try '--help' for more information.");
						System.exit(1);
						return;
				}
			}
		}

		logger.println("Initializing...");

		HTTPServer server = new HTTPServer(host, port, backlog, Executors.newFixedThreadPool(threads));
		server.setHandler(new SimpleHTTPHandler(baseDir));
		server.setLogger(logger);
		server.start();

		logger.println("HTTP server started on " + host + ":" + port + " (http://" + host + ":" + port + "/)");
	}
}
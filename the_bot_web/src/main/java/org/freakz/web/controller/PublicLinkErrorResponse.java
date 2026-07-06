package org.freakz.web.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

final class PublicLinkErrorResponse {

  private static final String NOT_FOUND_TITLE = "Link not found or expired";

  private PublicLinkErrorResponse() {
  }

  static ResponseEntity<String> notFound() {
    String body = """
        <!doctype html>
        <html lang="en">
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1">
          <title>Link not found or expired</title>
          <style>
            body {
              margin: 0;
              min-height: 100vh;
              display: grid;
              place-items: center;
              background: #202020;
              color: #f2f2f2;
              font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
            }
            main {
              width: min(32rem, calc(100vw - 2rem));
              padding: 2rem;
            }
            h1 {
              margin: 0 0 .75rem;
              font-size: 1.65rem;
            }
            p {
              margin: 0;
              color: #b8b8b8;
              line-height: 1.5;
            }
          </style>
        </head>
        <body>
          <main>
            <h1>Link not found or expired</h1>
            <p>The shared bot link does not exist, has expired, or is no longer available.</p>
          </main>
        </body>
        </html>
        """;
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .contentType(MediaType.TEXT_HTML)
        .header("X-Content-Type-Options", "nosniff")
        .cacheControl(CacheControl.noStore())
        .body(body);
  }

  static String notFoundTitle() {
    return NOT_FOUND_TITLE;
  }
}

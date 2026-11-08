package io.github.kizio806.spectraevents.adapter.delivery.modrinth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ModrinthApiClientTest {

  private HttpServer server;
  private String baseUrl;
  private HttpClient client;

  @BeforeEach
  void setUp() throws Exception {
    server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    server.start();
    baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
  }

  @AfterEach
  void tearDown() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void testSuccessfulProjectVersionsFetch() {
    server.createContext(
        "/project/my-pack/version",
        exchange -> {
          byte[] response = "[{\"version_number\":\"1.0.0\"}]".getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, response.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
          }
        });

    ModrinthApiClient apiClient = new ModrinthApiClient(baseUrl, client, "SpectraEvents/Test");
    String result = apiClient.getProjectVersions("my-pack", "paper", "26.1").join();
    assertEquals("[{\"version_number\":\"1.0.0\"}]", result);
  }

  @Test
  void testNotFoundThrowsIllegalStateException() {
    server.createContext(
        "/project/missing-pack/version",
        exchange -> {
          exchange.sendResponseHeaders(404, -1);
        });

    ModrinthApiClient apiClient = new ModrinthApiClient(baseUrl, client, "SpectraEvents/Test");
    CompletionException ex =
        assertThrows(
            CompletionException.class,
            () -> apiClient.getProjectVersions("missing-pack", "paper", "26.1").join());
    assertTrue(ex.getCause() instanceof IllegalStateException);
    assertTrue(ex.getCause().getMessage().contains("not found"));
  }

  @Test
  void testServerErrorThrowsIllegalStateException() {
    server.createContext(
        "/project/err-pack/version",
        exchange -> {
          exchange.sendResponseHeaders(500, -1);
        });

    ModrinthApiClient apiClient = new ModrinthApiClient(baseUrl, client, "SpectraEvents/Test");
    CompletionException ex =
        assertThrows(
            CompletionException.class,
            () -> apiClient.getProjectVersions("err-pack", "paper", "26.1").join());
    assertTrue(ex.getCause() instanceof IllegalStateException);
    assertTrue(ex.getCause().getMessage().contains("status 500"));
  }

  @Test
  void testConnectionFailureThrowsIllegalStateException() {
    // Port with nothing listening
    ModrinthApiClient apiClient =
        new ModrinthApiClient("http://127.0.0.1:1", client, "SpectraEvents/Test");
    CompletionException ex =
        assertThrows(
            CompletionException.class,
            () -> apiClient.getProjectVersions("any-pack", "paper", "26.1").join());
    assertTrue(ex.getCause() instanceof IllegalStateException);
  }

  @Test
  void testDefaultConstructorInstantiation() {
    ModrinthApiClient apiClient = new ModrinthApiClient("1.0.0");
    // Verifies constructor succeeds
    org.junit.jupiter.api.Assertions.assertNotNull(apiClient);
  }

  @Test
  void testInterruptedClientThrowsIllegalStateException() {
    try (HttpClient interruptingClient =
        new HttpClient() {
          @Override
          public java.util.Optional<java.net.CookieHandler> cookieHandler() {
            return java.util.Optional.empty();
          }

          @Override
          public java.util.Optional<Duration> connectTimeout() {
            return java.util.Optional.empty();
          }

          @Override
          public Redirect followRedirects() {
            return Redirect.NEVER;
          }

          @Override
          public java.util.Optional<java.net.ProxySelector> proxy() {
            return java.util.Optional.empty();
          }

          @Override
          public javax.net.ssl.SSLContext sslContext() {
            return null;
          }

          @Override
          public javax.net.ssl.SSLParameters sslParameters() {
            return null;
          }

          @Override
          public java.util.Optional<java.net.Authenticator> authenticator() {
            return java.util.Optional.empty();
          }

          @Override
          public Version version() {
            return Version.HTTP_2;
          }

          @Override
          public java.util.Optional<java.util.concurrent.Executor> executor() {
            return java.util.Optional.empty();
          }

          @Override
          public <T> java.net.http.HttpResponse<T> send(
              java.net.http.HttpRequest request,
              java.net.http.HttpResponse.BodyHandler<T> responseBodyHandler)
              throws InterruptedException {
            throw new InterruptedException("Simulated interruption");
          }

          @Override
          public <T>
              java.util.concurrent.CompletableFuture<java.net.http.HttpResponse<T>> sendAsync(
                  java.net.http.HttpRequest request,
                  java.net.http.HttpResponse.BodyHandler<T> responseBodyHandler) {
            return java.util.concurrent.CompletableFuture.failedFuture(
                new InterruptedException("Simulated interruption"));
          }

          @Override
          public <T>
              java.util.concurrent.CompletableFuture<java.net.http.HttpResponse<T>> sendAsync(
                  java.net.http.HttpRequest request,
                  java.net.http.HttpResponse.BodyHandler<T> responseBodyHandler,
                  java.net.http.HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
            return java.util.concurrent.CompletableFuture.failedFuture(
                new InterruptedException("Simulated interruption"));
          }
        }) {
      ModrinthApiClient apiClient =
          new ModrinthApiClient("http://127.0.0.1:8080", interruptingClient, "SpectraEvents/Test");
      CompletionException ex =
          assertThrows(
              CompletionException.class,
              () -> apiClient.getProjectVersions("my-pack", "paper", "26.1").join());
      assertTrue(ex.getCause() instanceof IllegalStateException);
      assertTrue(ex.getCause().getMessage().contains("Interrupted"));
    }
  }
}

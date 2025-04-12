package org.example;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class NioExampleTest {

  @Test
  void testSendMultipleRequests() throws IOException {
    NioExample nioExample = new NioExample(2);
    String serverUrl = "http://localhost:7000";
    List<CompletableFuture<Void>> requests = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      String requestUrl = serverUrl + "?id=" + i;
      if (i != 9) {
        requestUrl += "&wait=" + 5000;
      }
      requests.add(nioExample.sendRequest(requestUrl));
    }
    CompletableFuture.allOf(requests.toArray(new CompletableFuture[0])).join();
  }

  @Test
  void testReadFile() throws IOException, URISyntaxException {
    NioExample nioExample = new NioExample(1);
    System.out.println(nioExample.readFromFile("example.txt"));
  }

  @Test
  void testSendRequestWithSocketChannel() throws IOException {
    String serverUrl = "localhost";
    NioExample nioExample = new NioExample(1, serverUrl, 7000);
    for (int i = 0; i < 10; i++) {
      String requestUrl =  "/?id=" + i;
      if (i != 9) {
        requestUrl += "&wait=" + 5000;
      }
      nioExample.sendRequestUsingSocketChannel(serverUrl, requestUrl);
    }
    for (int i = 0; i < 10; i++) {
      System.out.println(nioExample.readResponseUsingSocketChannel());
    }
    nioExample.getSelector().close();
  }

}
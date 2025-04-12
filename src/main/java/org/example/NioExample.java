package org.example;

import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NioExample {

  private ExecutorService executor;
  private HttpClient client;
  private Selector selector;
  private static final String CRLF = "\r\n";

  public NioExample(int threadCount) throws IOException {
    setupHttpClient(threadCount);
  }

  public NioExample(int threadCount, String host, Integer port) throws IOException {
    this(threadCount);
    setupSelector(threadCount, host, port);
  }

  private void setupSelector(int threadCount, String host, int port) throws IOException {
    selector = Selector.open();
    for (int i = 0; i < 10; i++) {
      createChannel(host, port);
    }
  }

  private void setupHttpClient(int threadCount) {
    executor = Executors.newFixedThreadPool(threadCount);

    client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .executor(executor)
        .build();
  }

  public String readFromFile(String fileName) throws IOException, URISyntaxException {
    StringBuilder content = new StringBuilder();
    try (FileChannel inChannel = FileChannel.open(Path.of(fileName), StandardOpenOption.READ)) {
      ByteBuffer buf = ByteBuffer.allocate(1024);
      while (inChannel.read(buf) > 0) {
        buf.flip();
        content.append(UTF_8.decode(buf));
        buf.clear();
      }
    }
    return content.toString();
  }

  public CompletableFuture<Void> sendRequest(String url) {
    return client.sendAsync(HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .build(), ofString())
        .thenAcceptAsync(body -> {
          System.out.println(body + " Executed in " + Thread.currentThread().getName());
        }, executor)
        .exceptionally(exception -> {
          System.out.println("something went wrong");
          return null;
        });
  }

  public void sendRequestUsingSocketChannel(String host, String path)
      throws IOException {
    SocketChannel writableChannel = null;
    SelectionKey selectedKey = null;
    while(writableChannel == null) {
      int select = selector.select();
      Set<SelectionKey> selectionKeys = selector.selectedKeys();
      Iterator<SelectionKey> iterator = selectionKeys.iterator();
      while (iterator.hasNext()) {
        SelectionKey selectionKey = iterator.next();
        if (selectionKey.isConnectable()) {
          selectedKey = selectionKey;
          SocketChannel channel = (SocketChannel) selectionKey.channel();
          channel.finishConnect();
          selectedKey.interestOps(SelectionKey.OP_WRITE);
          iterator.remove();
        } else if (selectionKey.isWritable()) {
          selectedKey = selectionKey;
          writableChannel = (SocketChannel) selectionKey.channel();
          iterator.remove();
          break;
        }
      }
    }
    writableChannel.write(UTF_8.encode("GET " + path + " HTTP/1.1" + CRLF));
    writableChannel.write(UTF_8.encode("Host: " + host + CRLF));
    writableChannel.write(UTF_8.encode("Connection: close" + CRLF));
    writableChannel.write(UTF_8.encode(CRLF));
    selectedKey.interestOps(SelectionKey.OP_READ);
  }

  public String readResponseUsingSocketChannel() throws IOException {
    SocketChannel readableChannel = null;
    while(readableChannel == null) {
      int select = selector.select();
      Set<SelectionKey> selectionKeys = selector.selectedKeys();
      Iterator<SelectionKey> iterator = selectionKeys.iterator();
      while (iterator.hasNext()){
        SelectionKey selectionKey = iterator.next();
        if (selectionKey.isReadable()) {
          readableChannel = (SocketChannel) selectionKey.channel();
          break;
        }
      }
    }
    StringBuilder response = new StringBuilder();
    ByteBuffer buf = ByteBuffer.allocate(1024);
    while(readableChannel.read(buf) != -1) {
      buf.flip();
      response.append(UTF_8.decode(buf));
      buf.clear();
    }
    readableChannel.close();
    return response.toString();
  }

  private void createChannel(String host, int port) throws IOException {
    SocketChannel socketChannel = SocketChannel.open();
    socketChannel.configureBlocking(false);
    socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_CONNECT | SelectionKey.OP_WRITE);
    socketChannel.connect(new InetSocketAddress(InetAddress.getByName(host), port));
  }

  public Selector getSelector(){
    return this.selector;
  }
}

package org.example;

import java.io.IOException;
import java.net.URISyntaxException;

public class Main {

  public static void main(String[] args) throws IOException {
    NioExample nioExample = new NioExample(1);
    try {
      System.out.println("Hell " + nioExample.readFromFile("example.txt"));
    } catch (IOException | URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
package mixer;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import deriggy.api.Deriggy;
import deriggy.api.Edge;
import deriggy.api.Id;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.fluo.api.client.FluoClient;
import org.apache.fluo.api.client.FluoFactory;
import org.apache.fluo.api.client.LoaderExecutor;
import org.apache.fluo.api.client.Snapshot;
import org.apache.fluo.api.config.FluoConfiguration;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public class Shell {

  private static Edge parseEdge(String[] tokens) {
    return new Edge(tokens[1], tokens[2], tokens[3], "follows");
  }

  public static void main(String[] args) throws Exception {
    FluoConfiguration config = new FluoConfiguration(new File(args[0]));
    Deriggy deriggy = new Deriggy();

    try (FluoClient client = FluoFactory.newClient(config);
        LoaderExecutor loader = client.newLoaderExecutor()) {

      Terminal terminal = TerminalBuilder.terminal();
      Completer completer = (reader, line, candidates) -> {
        if (line.wordIndex() == 0) {
          Stream.of("follow", "unfollow", "alias", "unalias", "print", "exit", "help", "lookup",
              "setattrs").map(s -> new Candidate(s)).forEach(candidates::add);
        }
      };
      LineReader lineReader = LineReaderBuilder.builder().appName("mixer").completer(completer)
          .terminal(terminal).build();

      Connector conn =
          new ZooKeeperInstance(config.getAccumuloInstance(), config.getAccumuloZookeepers())
              .getConnector("root", "secret");

      String line;
      while ((line = lineReader.readLine(">")) != null) {

        String[] tokens = line.split("\\s+");

        switch (tokens[0]) {
          case "follow":
            loader.execute((tx, ctx) -> {
              deriggy.addEdge(tx, parseEdge(tokens));
            });
            break;
          case "unfollow":
            loader.execute((tx, ctx) -> {
              deriggy.removeEdge(tx, parseEdge(tokens));
            });
            break;
          case "alias":
            loader.execute((tx, ctx) -> {
              deriggy.setAlias(tx, new Id(tokens[1], tokens[2]), tokens[3]);
            });
            break;
          case "unalias":
            loader.execute((tx, ctx) -> {
              deriggy.removeAlias(tx, new Id(tokens[1], tokens[2]));
            });
            break;
          case "print":
            try (Snapshot snap = client.newSnapshot()) {
              snap.scanner().build().forEach(System.out::println);
            }
            break;
          case "load":
            java.util.Scanner lines = new java.util.Scanner(new File(tokens[2]));
            while (lines.hasNextLine()) {
              String[] edge = lines.nextLine().split("\\s+");
              loader.execute((tx, ctx) -> deriggy.addEdge(tx,
                  new Edge(tokens[1], edge[0], edge[1], "follows")));
            }

            break;
          case "lookup":
            String node = tokens[1];
            if (!node.contains(":")) {
              node = "alias:" + node;
            }
            Scanner scanner = conn.createScanner("sgraph", Authorizations.EMPTY);
            scanner.setRange(new Range(node));
            for (Entry<Key, Value> entry : scanner) {

              Key k = entry.getKey();
              String dir = k.getColumnFamilyData().toString().equals("following") ? "->" : "<-";
              String otherNode = k.getColumnQualifierData().toString();
              if (otherNode.startsWith("alias:")) {
                otherNode = otherNode.substring(6);
              }
              System.out.printf("  %s %s %-10s %s\n", tokens[1], dir, otherNode, entry.getValue());

              // System.out.println(entry.getKey() + " " + entry.getValue());
            }
            scanner.close();
            break;
          case "setattrs":
            Map<String, String> attrs = new HashMap<>();
            for (int i = 3; i < tokens.length; i++) {
              String[] kv = tokens[i].split("=");
              attrs.put(kv[0], kv[1]);
            }
            loader.execute((tx, ctx) -> {
              deriggy.setAttributes(tx, new Id(tokens[1], tokens[2]), attrs);
            });
            break;
          case "exit":
          case "quit":
            return;
          case "":
            break;
          case "help":
          default:
            System.out.println("Commands : ");
            System.out.println("");
            System.out.println("\tfollow <graph> <user id 1> <user id 2>");
            System.out.println("\tunfollow <graph> <user id 1> <user id 2>");
            System.out.println("\talias <graph> <user id> <alias>");
            System.out.println("\tunalias <graph> <user id>");
            System.out.println("\tlookup <id>");
            System.out.println("\tload <graph> <file>");
            System.out.println("\tsetattrs <graph> <id> {<key>=<value>}");
            System.out.println("\tprint");
            System.out.println("\thelp");
            System.out.println("\texit|quit");
            break;

        }
      }
    }
  }
}

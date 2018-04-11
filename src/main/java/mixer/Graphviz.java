package mixer;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.fluo.api.config.FluoConfiguration;

public class Graphviz {
  public static void main(String[] args) throws Exception {
    FluoConfiguration config = new FluoConfiguration(new File(args[0]));

    Connector conn =
        new ZooKeeperInstance(config.getAccumuloInstance(), config.getAccumuloZookeepers())
            .getConnector("root", "secret");

    Scanner scanner = conn.createScanner("sgraph", Authorizations.EMPTY);
    System.out.println("digraph sgraph {");
    System.out.println("\toverlap=scale");
    System.out.println("\tsplines=true");
    System.out.println("\tsep=\"+10,10\"");

    for (Entry<Key, Value> entry : scanner) {
      Key key = entry.getKey();
      if (key.getColumnFamilyData().toString().equals("following")) {
        String node1 = key.getRowData().toString();
        String node2 = key.getColumnQualifierData().toString();
        node1 = node1.replaceFirst("alias:", "");
        node2 = node2.replaceFirst("alias:", "");

        String encAttrs[] = entry.getValue().toString().split(",");
        Map<String, String> attrs = new HashMap<>();
        for (String attr : encAttrs) {
          String[] kv = attr.split("=");
          attrs.put(kv[0], kv[1]);
        }
        int numRawEdges = Integer.parseInt(attrs.getOrDefault("rawEdges","0"));
        String props = "";
        if (numRawEdges > 1) {
          props = String.format("[label=%d]", numRawEdges);
        }

        System.out.printf("\t\"%s\" -> \"%s\" %s\n", node1, node2, props);
      }
    }

    System.out.println("}");
    scanner.close();
  }
}

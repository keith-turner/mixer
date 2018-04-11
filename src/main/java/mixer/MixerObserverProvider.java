package mixer;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import deriggy.api.Deriggy;
import org.apache.accumulo.core.data.Mutation;
import org.apache.fluo.api.observer.ObserverProvider;
import org.apache.fluo.recipes.accumulo.export.function.AccumuloExporter;
import org.apache.fluo.recipes.accumulo.export.function.AccumuloTranslator;
import org.apache.fluo.recipes.core.export.ExportQueue;

public class MixerObserverProvider implements ObserverProvider {

  @Override
  public void provide(Registry registry, Context ctx) {

    ExportQueue<String, NodeChanges> exportQueue =
        ExportQueue.getInstance("gu", ctx.getAppConfiguration());


    // Observes the dervied graph and places changes on an export queue.
    Deriggy.registerObservers(registry, new MixerNodeObserver(exportQueue));

    // Translates derived graph changes into mutations for the external query table.
    AccumuloTranslator<String, NodeChanges> translator = (export, consumer) -> {
      String node = export.getKey();
      NodeChanges nodeChgs = export.getValue();
      Stream<String> stream1 =
          Stream.of("followers=" + nodeChgs.followers, "following=" + nodeChgs.following);
      Stream<String> stream2 =
          nodeChgs.attrs.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue());
      String val = Stream.concat(stream1, stream2).collect(Collectors.joining(","));


      for (NeighborChange nc : export.getValue().neighborChanges) {
        Mutation mutation = new Mutation(nc.neighborId);
        String fam = nc.following ? "follower" : "following";
        if (nc.delete) {
          mutation.putDelete(fam, node, export.getSequence());
        } else {
          mutation.put(fam, node, export.getSequence(), val + ",rawEdges=" + nc.numRawEdges);
        }

        consumer.accept(mutation);
      }
    };

    // Configure the export queue to send entries to an external accumulo query table
    exportQueue.registerObserver(registry,
        new AccumuloExporter<>("gu", ctx.getAppConfiguration(), translator));
  }
}

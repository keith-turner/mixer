package mixer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fluo.api.client.TransactionBase;
import org.apache.fluo.recipes.core.export.ExportQueue;

import deriggy.api.NodeObserver;

public class MixerNodeObserver implements NodeObserver {

  private ExportQueue<String, NodeChanges> exportQueue;

  public MixerNodeObserver(ExportQueue<String, NodeChanges> exportQueue) {
    this.exportQueue = exportQueue;
  }

  @Override
  public Map<String, String> process(TransactionBase tx, NodeState nodeState) {

    int numFollowing = 0;
    int numFollowers = 0;

    for (AliasEdge aliasEdge : nodeState.getAliasEdges()) {

      if (!aliasEdge.type.equals("follows") || aliasEdge.change == Change.DELETED) {
        continue;
      }

      if (aliasEdge.direction == Direction.OUT) {
        numFollowing++;
      } else {
        numFollowers++;
      }
    }

    List<NeighborChange> neighborChanges = new ArrayList<>();

    for (AliasEdge aliasEdge : nodeState.getAliasEdges()) {

      String otherId = aliasEdge.otherNode.graphId + ":" + aliasEdge.otherNode.nodeId;
      boolean following = aliasEdge.direction == Direction.OUT;
      boolean delete = aliasEdge.change == Change.DELETED;

      int numRawEdges = aliasEdge.sourceEdges.size();

      neighborChanges.add(new NeighborChange(otherId, numRawEdges, following, delete));
    }

    Map<String, String> attrs = new HashMap<>();
    nodeState.getAttributes().forEach((k,m)->{
      m.forEach((id,v) ->{
        attrs.merge(k, v, (v1,v2)->v1+","+v2);
      });
    });

    String myId = nodeState.getNodeId().graphId + ":" + nodeState.getNodeId().nodeId;
    NodeChanges nodeChanges = new NodeChanges(numFollowing, numFollowers, neighborChanges, attrs);
    exportQueue.add(tx, myId, nodeChanges);

    return Collections.emptyMap();
  }
}

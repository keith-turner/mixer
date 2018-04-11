package mixer;

import java.util.List;
import java.util.Map;

public class NodeChanges {
  int following;
  int followers;

  List<NeighborChange> neighborChanges;
  Map<String,String> attrs;

  public NodeChanges() {}

  public NodeChanges(int following, int followers, List<NeighborChange> neighborChanges, Map<String, String> attrs) {
    super();
    this.following = following;
    this.followers = followers;
    this.neighborChanges = neighborChanges;
    this.attrs = attrs;
  }

  @Override
  public String toString() {
    return String.format("[followers:%d following:%d  NC:%s]", followers, following,
        neighborChanges);
  }

}

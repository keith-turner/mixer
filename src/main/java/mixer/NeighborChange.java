package mixer;

public class NeighborChange {
  public boolean delete;
  public boolean following;
  public String neighborId;
  public int numRawEdges;

  public NeighborChange() {}

  public NeighborChange(String neighborId, int numRawEdges, boolean following, boolean delete) {
    this.neighborId = neighborId;
    this.numRawEdges = numRawEdges;
    this.following = following;
    this.delete = delete;
  }

  @Override
  public String toString() {
    return String.format("%s %d %s %s", neighborId, numRawEdges, following, delete);
  }
}

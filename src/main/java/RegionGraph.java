import javax.swing.plaf.synth.Region;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

public class RegionGraph {
    public ArrayList<Node> nodes;
    public HashSet<Node> regionNodes;
    RegionGraph(){
        this.regionNodes = new HashSet<>();
        this.nodes = new ArrayList<>();
    }

    RegionGraph(Collection nodes){
        this();
        this.nodes.addAll(nodes);
    }

    public HashSet<Node> getRegionNodes() {
        return regionNodes;
    }

    public void removeNode(Node n){
        for(Node neighbour:n.neighbours){
            neighbour.neighbours.remove(n);
        }
        this.regionNodes.remove(n);
        this.nodes.remove(n);
    }

    public boolean setNodeAsRegion(Node n){
        if(nodes.contains(n)){
            this.regionNodes.add(n);
            return true;
        }
        return false;
    }
}

import java.util.HashSet;

public class Node {
    public HashSet<Node> neighbours;
    public Point position;
    public double distanceToObstacle;
    public Node(){
        this.neighbours = new HashSet<>();
        this.position = new Point();
    }
    public Node(Point p){
        this.neighbours = new HashSet<>();
        this.position = p;
    }
    public Node(int x, int y){
        this.neighbours = new HashSet<>();
        this.position = new Point(x,y);
    }
    public void addNeighbour(Node n){
        this.neighbours.add(n);
    }
}

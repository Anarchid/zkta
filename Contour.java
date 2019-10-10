import java.util.ArrayList;

public class Contour {
    ArrayList<Point> points;
    Contour(){
        this.points = new ArrayList<Point>();
    }
    void addPoint(Point p){
        this.points.add(p);
    }
    void addPoint(int x, int y){
        this.points.add(new Point(x,y));
    }
    ArrayList<Point> getPoints(){
        return this.points;
    }
    Point getPoint(int p){
        return this.points.get(p);
    }
    int length(){
        return this.points.size();
    }
}

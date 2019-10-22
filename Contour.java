import java.util.ArrayList;
import java.util.Collections;

public class Contour {
    ArrayList<Point> points;
    Contour(){
        this.points = new ArrayList<Point>();
    }
    Contour(ArrayList<Point>p){
        this.points = p;
    }
    Contour(Point[] pts){
        this();
        for(Point p:pts){
            this.points.add(p);
        }
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

    Point[] getPointsArray(){
        Point[] a = new Point[this.points.size()];
        return this.points.toArray(a);
    }

    Point getPoint(int p){
        return this.points.get(p);
    }
    int length(){
        return this.points.size();
    }
}

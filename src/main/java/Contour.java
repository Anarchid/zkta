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

    int[] getBounds(){
        if(points.size() == 0){
            return null;
        }
        int minX=Integer.MAX_VALUE,minY=Integer.MAX_VALUE,maxX=0, maxY=0;
        for(Point p: points){
            minX = Math.min(p.x,minX);
            maxX = Math.max(p.x,maxX);
            minY = Math.min(p.y,minY);
            maxY = Math.max(p.y,maxY);
        }
        return new int[]{minX, maxX,minY, maxY};
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

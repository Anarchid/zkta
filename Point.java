public class Point {
    public int x;
    public int y;

    Point(){
        this.x = 0;
        this.y = 0;
    }

    Point(int x, int y){
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return "<"+this.x+","+this.y+">";
    }
}

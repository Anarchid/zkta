public class TracePoint extends Point {
    public int traceDirection;
    TracePoint(int x, int y, int traceDirection){
        this.traceDirection = traceDirection;
        this.x = x;
        this.y = y;
    }
}

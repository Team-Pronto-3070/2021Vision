public class PixelPoint {
    
    final double x;
    final double y;
    final double area;

    public PixelPoint(double x, double y, double area){
        this.x = x;
        this.y = y;
        this.area = area;
    }

    public double getDifference(PixelPoint point){
        double sum = 0;

        sum += Math.abs(Math.abs(x) - Math.abs(point.x)); 
        sum += Math.abs(Math.abs(y) - Math.abs(point.y)); 
        sum += Math.abs(Math.abs(area) - Math.abs(point.area)); 

        return sum;
    }
}

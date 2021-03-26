public class PixelProfile {
    
    public final PixelPoint[] profile;

    public PixelProfile(PixelPoint[] points){
        profile = points;
    }

    public double compare(PixelProfile otherProfile){
        return 0.0;
    }
}

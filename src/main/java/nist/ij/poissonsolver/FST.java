package nist.ij.poissonsolver;

import ij.ImagePlus;
import ij.ImageStack;
import ij.Undo;
import ij.process.ColorProcessor;
import ij.process.FHT;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

public class FST extends FloatProcessor{
	public ImageProcessor ip;
	public int edgeLength;
	public boolean padded;
	
	public FST(ImageProcessor ip) {
		super(ip.getWidth(), ip.getHeight(), (float[])((ip instanceof FloatProcessor)?ip.duplicate().getPixels():ip.convertToFloat().getPixels()), null);
		this.ip = ip;
		resetRoi();
	}

	public ImageProcessor calculate() {
		boolean notSquare = !checkSquare();
		
		if (notSquare) {
			//IJ.error("A square image is required for this procedure. Try cropping the image to an nxn pixel size.");
		}
		
		FHT fht;
		ImageStack fhtComplex = null;

		fht = newFHT(ip);
		fht.transform();
		fht.swapQuadrants();
		fhtComplex = fht.getComplexTransform();
		this.ip.setPixels(fhtComplex.getPixels(2));
		ip.setRoi(1,1,this.edgeLength+1,this.edgeLength+1);
		
		//new ImagePlus("Complex Transform", fhtComplex).show();
		
		this.ip = ip.crop();

		//new ImagePlus("Imaginary Component", ip).show();
		
		return ip;
	}
	
	private boolean checkSquare() {
		this.edgeLength = ip.getWidth();
		return ip.getWidth()==ip.getHeight();
	}
	
    private FHT newFHT(ImageProcessor ip) {
        FHT fht;
        if (ip instanceof ColorProcessor) {
            ImageProcessor ip2 = ((ColorProcessor)ip).getBrightness();
            fht = new FHT(pad(ip2));
            fht.rgb = (ColorProcessor)ip.duplicate(); // save so we can later update the brightness
        } else
            fht = new FHT(pad(ip));
        if (padded) {
            fht.originalWidth = edgeLength;
            fht.originalHeight = edgeLength;
        }
        fht.originalBitDepth = ip.getBitDepth();
        fht.originalColorModel = ip.getColorModel();
        return fht;
    }
    
    private ImageProcessor pad(ImageProcessor ip) {
        int maxN = edgeLength;
        int i = 2;
        while(i<maxN) i *= 2;
        if (i==maxN) {
            padded = false;
            return ip;
        }
        maxN = i;
        ImageStatistics stats = ImageStatistics.getStatistics(ip, ImageStatistics.MEAN, null);
        ImageProcessor ip2 = ip.createProcessor(maxN, maxN);
        ip2.setValue(stats.mean);
        ip2.fill();
        ip2.insert(ip, 0, 0);
        padded = true;
        Undo.reset();
        //new ImagePlus("padded", ip2.duplicate()).show();
        return ip2;
    }
    
    public ImageProcessor getProcessor() {
    	return ip;
    }
}

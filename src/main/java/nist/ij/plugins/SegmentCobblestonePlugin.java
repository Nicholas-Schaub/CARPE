package nist.ij.plugins;

import fiji.threshold.Auto_Local_Threshold;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.plugin.filter.Binary;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.FloodFiller;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.ShortProcessor;
import mpicbg.ij.integral.NormalizeLocalContrast;
import mpicbg.ij.integral.RemoveOutliers;
import steerablej.SteerableDetector;

public class SegmentCobblestonePlugin implements PlugIn {
	protected ImagePlus image;
	protected static String imageName = "";

	protected FloatProcessor pip = null;
	protected static final int flags = 29;
	
	public static void main(String[] args) {
		// set the plugins.dir property to make the plugin appear in the Plugins menu
		Class<?> clazz = SegmentCobblestonePlugin.class;
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
		System.setProperty("plugins.dir", pluginsDir);

		// start ImageJ
		new ImageJ();

		// open the Clown sample
		ImagePlus image = IJ.openImage("C:\\Program Files\\Micro-Manager-1.4\\images\\Cobblestone.tif");
		image.show();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	}


	@Override
	public void run(String arg) {
		// TODO Auto-generated method stub
		image = IJ.getImage();
		pip = image.getProcessor().convertToFloatProcessor();
		//new ImagePlus("Original Image", pip.duplicate()).show();
		
		RemoveOutliers rmo = new RemoveOutliers(pip);
		rmo.removeOutliers(10,10,1F);
		//new ImagePlus("Remove Outliers",pip.duplicate()).show();
		
		NormalizeLocalContrast nlc = new NormalizeLocalContrast(pip);
		nlc.run(40,40,1F,true,false);
		//new ImagePlus("Normalize Contrast",pip.duplicate()).show();
		
		float[] fpix = (float[]) pip.getPixels();
		double[] dpix = new double[fpix.length];
		
		for (int i=0; i<fpix.length; i++) {
			dpix[i] = (double) fpix[i];
		}
		
		double[] alpha = new double[5];
		alpha[0] = 0.113;
		alpha[1] = -0.392;
		alpha[2] = 0.025;
		alpha[3] = -0.184;
		alpha[4] = 0.034;
		SteerableDetector sd = new SteerableDetector(dpix, pip.getWidth(), pip.getHeight(), 1, 2, 4, alpha);
		sd.run();
		
		dpix = sd.getResponse();
		
		for (int i=0; i<fpix.length; i++) {
			fpix[i] = (float) dpix[i];
		}
		
		pip.setPixels(fpix);
		ImageStatistics imstat = pip.getStatistics();
		double imMax = imstat.max;
		double imMin = imstat.min;
		pip.setMinAndMax(imMin, imMax);
		ByteProcessor pipByte = pip.convertToByteProcessor();
		image = new ImagePlus("Steerable Filter",pipByte);
		//image.show();
		
		Auto_Local_Threshold alt = new Auto_Local_Threshold();
		
		alt.exec(image, "Otsu", 10, 0, 0, true);
		pip = image.getProcessor().convertToFloatProcessor();
		ShortProcessor pipShort = pipByte.convertToShortProcessor(true); 
		//fill(pipShort, 0, 255);
		
		pipByte = pipShort.convertToByteProcessor();
		pipByte.invert();
		//pipByte.skeletonize();
		pipByte.invert();
		
		new ImagePlus("Filled Image", pipByte).show();
	}
	
    void fill(ImageProcessor ip, int foreground, int background) {
        int width = ip.getWidth();
        int height = ip.getHeight();
        FloodFiller ff = new FloodFiller(ip);
        
        int maxColor = (int) (Math.pow(2, 15))-1;
        int currentColor = maxColor;
        ip.setColor(currentColor);
        for (int y=0; y<height; y++) {
            if (ip.getPixel(0,y)==background) {ff.fill(0, y); ip.setColor(--currentColor);}
            if (ip.getPixel(width-1,y)==background) {ff.fill(width-1, y); ip.setColor(--currentColor);}
        }
        for (int x=0; x<width; x++){
            if (ip.getPixel(x,0)==background) {ff.fill(x, 0); ip.setColor(--currentColor);}
            if (ip.getPixel(x,height-1)==background) {ff.fill(x, height-1); ip.setColor(--currentColor);}
        }
        short[] pixels = (short[])ip.getPixels();
        int n = width*height;
        int[] colorSize = new int[(maxColor - currentColor)];
        
        for (int i=0; i<colorSize.length; i++) {
        	colorSize[i] = 0;
        }
        
        for (int i=0; i<n; i++) {
        	int pixelInt = new Short(pixels[i]).intValue();
	        if (pixelInt==255 || pixelInt==0) {pixels[i] = (short)foreground;}
	        else {colorSize[maxColor-pixelInt] += 1;}
        }
        
        int maxSize = colorSize[0];
        int biggestColor = maxColor-1;
        for (int i=1; i<colorSize.length; i++) {
        	if (maxSize<colorSize[i]) {maxSize = colorSize[i]; biggestColor = maxColor-i;}
        }
        
        for (int i=0; i<n; i++) {
        	int pixelInt = new Short(pixels[i]).intValue();
	        if (pixelInt!=biggestColor) {pixels[i] = (short)foreground;}
        }
    }

}

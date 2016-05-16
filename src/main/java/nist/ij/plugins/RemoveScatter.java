package nist.ij.plugins;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import ij.IJ;
import ij.ImagePlus;
import ij.measure.CurveFitter;
import ij.plugin.PlugIn;
import ij.process.FloatProcessor;

public class RemoveScatter implements PlugIn {
	protected ImagePlus image;
	protected static String imageName = "";

	protected FloatProcessor pip = null;
	protected static final int flags = 29;
	Double[] red;

	@Override
	public void run(String arg) {
		long startTime = System.currentTimeMillis();
		image = IJ.getImage();
		pip = image.getProcessor().convertToFloatProcessor();
		int channels = image.getNChannels();
		int width = image.getWidth();
		int height = image.getHeight();
		
		double[] lambda = {(double) 640, (double) 548, (double) 496};
		ArrayList<Double[]> dpix = image2Double(image);
		int imgSize = dpix.get(0).length;
		ExecutorService cfThreads = Executors.newFixedThreadPool(7);
		
		double[] spix = new double[imgSize];
		double[] mpix = new double[imgSize];
		
		for (int i=0; i<imgSize; i++) {
			double[] x = new double[channels];
			double[] y = new double[channels];
			for (int j=0; j<dpix.size(); j++) {
				x[j] = lambda[j];
				y[j] = dpix.get(j)[i];
			}
			cfThreads.execute(new PixReg(x,y,spix,mpix,i));
		}
		
		cfThreads.shutdown();
		try {
			cfThreads.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		FloatProcessor sP = new FloatProcessor(width,height,spix);
		FloatProcessor mP = new FloatProcessor(width,height,mpix);
		
		ImagePlus sI = new ImagePlus("Scatter",sP);
		ImagePlus mI = new ImagePlus("Melanin",mP);
		
		sI.show();
		mI.show();
		long stopTime = System.currentTimeMillis();
		IJ.showProgress(1);
		System.out.println("Time to completion (ms): " + Long.toString(stopTime-startTime));
	}

	public ArrayList<Double[]> image2Double(ImagePlus imp) {
		int channels = imp.getNChannels();
		ArrayList<Double[]> dpix = new ArrayList<Double[]>();
		for (int i=0; i<channels; i++) {
			imp.setPosition(i+1);
			float[] fpix = (float[]) imp.getProcessor().getPixels();
			dpix.add(new Double[fpix.length]);
			for (int j=0; j<fpix.length; j++) {
				dpix.get(i)[j] = (double) fpix[j];
			}
		}
		return dpix;
	}
	
	class PixReg implements Runnable {

		private CurveFitter cf;
		double[] spix;
		double[] mpix;
		int index;
		
		public PixReg(double[] x, double[] y, double[] spix, double[] mpix, int index) {
			cf = new CurveFitter(x,y);
			this.spix = spix;
			this.mpix = mpix;
			this.index = index;
		}

		public void run() {
			if (index%1000==0) {
				System.out.println("Current pixel: " + index);
				IJ.showProgress(index/(double) spix.length);
			}
			String fitEquation = "y = a + b*0.1974*exp(-0.0045*x)";
			double[] initParams = {cf.getYPoints()[2],cf.getYPoints()[2]/(0.1974*Math.exp(-.0045*cf.getXPoints()[2]))};
			cf.doCustomFit(fitEquation, initParams, false);
			spix[index] = cf.getParams()[0];
			mpix[index] = cf.getParams()[1];
		}
		
	}
}

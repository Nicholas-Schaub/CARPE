package nist.ij.integralimage;

import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import mpicbg.ij.integral.DoubleIntegralImage;
import mpicbg.ij.integral.IntegralImage;
import mpicbg.ij.integral.LongIntegralImage;
import mpicbg.ij.integral.LongRGBIntegralImage;

public final class IntIm2D {
	private final IntegralImage integral;
	private final ImageProcessor ip;
	private ImageProcessor pip;
	private final String direction;
	private boolean isFloat = false;
	
	public IntIm2D(ColorProcessor ip, String dir) {
		this.ip = ip;
		this.direction = dir;
		integral = new LongRGBIntegralImage(ip);
	}
	
	public IntIm2D(ByteProcessor ip, String dir) {
		this.ip = ip;
		this.direction = dir;
		integral = new LongIntegralImage(ip);
	}
	
	public IntIm2D(ShortProcessor ip, String dir) {
		this.ip = ip;
		this.direction = dir;
		integral = new LongIntegralImage(ip);
	}
	
	public IntIm2D(FloatProcessor ip, String dir) {
		isFloat = true;
		this.ip = ip;
		this.direction = dir;
		integral = new DoubleIntegralImage(ip);
	}
	
	public final void calculate(int featureSize) {
		if (isFloat) {
			pip = ip;
		} else {
			pip = ip.convertToFloatProcessor();
		}
		
		int w = pip.getWidth() - 1;
		int h = pip.getHeight() - 1;
		int minSize = (int) Math.floor(((double) featureSize-1)/2);
		int maxSize = (int) Math.ceil(((double) featureSize-1)/2);
		
		if (direction == "dxdy") {
			calculateMixed(w,h,minSize,maxSize, featureSize);
			return;
		}
		for 
		(int y=0; y<=h; y++) {
			
			int ySubMin = Math.max(-1, y - minSize - 1);
			int ySubMax = Math.min(h, y + maxSize);
			
			int yAddMin = Math.max(-1, y - minSize - featureSize - 1);
			int yAddMax = Math.min(h, y + maxSize + featureSize);
			
			if (direction=="dx^2"){
				ySubMin = yAddMin;
				ySubMax = yAddMax;
			}
			
			for (int x=0; x<=w; x++) {
				
				int xSubMin = Math.max(-1, x - minSize - 1);
				int xSubMax = Math.min(w, x + maxSize);
				
				int xAddMin = Math.max(-1, x - minSize - featureSize - 1);
				int xAddMax = Math.min(w, x + maxSize + featureSize);
				
				if (direction=="dy^2") {
					xSubMin = xAddMin;
					xSubMax = xAddMax;
				}

				pip.putPixelValue(x, y, (double) integral.getSum(xAddMin, yAddMin, xAddMax, yAddMax) - (double) 3 * integral.getSum(xSubMin, ySubMin, xSubMax, ySubMax));
			}
		}
		
		if (ip instanceof ByteProcessor) {
			ip.setPixels(pip.convertToByte(false).getPixels());
		} else if (ip instanceof ShortProcessor) {
			ip.setPixels(pip.convertToShort(false).getPixels());
		} else {
			ip.setPixels(pip.getPixels());
		}
			
	}
	
	private void calculateMixed(int w,int h,int minSize,int maxSize, int featureSize) {
		
		for (int y=0; y<=h; y++) {
			
			int yAddUpperMin = Math.max(-1, y - featureSize - minSize - 1);
			int yAddUpperMax = y-maxSize+minSize;
			
			int yAddLowerMin = (int) Math.min(h, yAddUpperMin+featureSize+maxSize);
			int yAddLowerMax = (int) Math.min(h, yAddUpperMax+featureSize+maxSize);
			
			for (int x=0; x<=w; x++) {
				double totalSum = 0.0;
				
				int xAddUpperMin = Math.max(-1, x - featureSize - minSize - 1);
				int xAddUpperMax = x-maxSize+minSize;
				
				int xAddLowerMin = (int) Math.min(w, xAddUpperMin+featureSize+maxSize);
				int xAddLowerMax = (int) Math.min(w, xAddUpperMax+featureSize+maxSize);

				totalSum += (double) integral.getSum(xAddUpperMin, yAddUpperMin, xAddUpperMax, yAddUpperMax);
				totalSum -= (double) integral.getSum(xAddLowerMin, yAddUpperMin, xAddLowerMax, yAddUpperMax);
				totalSum -= (double) integral.getSum(xAddUpperMin, yAddLowerMin, xAddUpperMax, yAddLowerMax);
				totalSum += (double) integral.getSum(xAddLowerMin, yAddLowerMin, xAddLowerMax, yAddLowerMax);
				
				pip.putPixelValue(x, y, totalSum);
			}
		}
		
		if (ip instanceof ByteProcessor) {
			ip.setPixels(pip.convertToByte(false).getPixels());
		} else if (ip instanceof ShortProcessor) {
			ip.setPixels(pip.convertToShort(false).getPixels());
		} else {
			ip.setPixels(pip.getPixels());
		}
	}
	
	public static final IntIm2D create(ImageProcessor ip, String dir) {
		if (FloatProcessor.class.isInstance(ip))
			return new IntIm2D((FloatProcessor)ip, dir);
		if (ByteProcessor.class.isInstance(ip))
			return new IntIm2D((ByteProcessor)ip, dir);
		if (ShortProcessor.class.isInstance(ip))
			return new IntIm2D((ShortProcessor)ip, dir);
		if (ColorProcessor.class.isInstance(ip)) {
			return new IntIm2D((ColorProcessor)ip, dir);
		}
		return null;
	}
	
	public final FloatProcessor createFloat() {
		return pip.convertToFloatProcessor();
	}
}

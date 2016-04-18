package nist.ij.poissonsolver;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FHT;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

public final class Poisson2D {
	public ImageProcessor ip;
	public int edgeLength;
	public boolean padded;
	
	public Poisson2D(ImageProcessor ip) {
		this.ip = ip.convertToFloatProcessor();
		this.edgeLength = ip.getWidth();
	}

	public ImageProcessor calculate() {
		
		FST fst = new FST(ip);
		fst = new FST(fst.calculate());
		ip = fst.calculate();
		
/*		float[] fpix = (float[]) fst.getPixels();
		
		int index = 0;
		double lambda1;
		double lambda2;
		
		for (int i = 0; i<this.edgeLength; i++) {
			lambda1 = 2*(1-Math.cos(i*Math.PI/(this.edgeLength+1)));
			for (int j = 0; j<this.edgeLength; j++) {
				lambda2 = 2*(1-Math.cos(j*Math.PI/(this.edgeLength+1)));
				fpix[index] *= (float) ((2/(this.edgeLength+1))/(lambda1+lambda2));
			}
		}
		
		fst.setPixels(fpix);
		
		fst = new FST(fst.calculate());*/
		
		return fst;
	}
	
	public ImageProcessor getProcessor() {
		return this.ip;
	}
}

package nist.ij.integralimage;


import ij.IJ;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

public class IntImVesselness {
	private final int minTube;
	private final int maxTube;
	private final ImageProcessor ip;
	private ImageProcessor pip;
	private ImagePlus calculatedValues;
	private int w = 0;
	private int h = 0;
	private boolean complete = false;
	private ImagePlus vMaxImg =  null;
	
	public IntImVesselness(ColorProcessor ip, int minTube, int maxTube) {
		this.ip = ip;
		this.minTube = (int) (Math.floor(minTube/2)*2+1);
		this.maxTube = ((int) Math.floor(maxTube/2)*2+1);
	}
	
	public IntImVesselness(ByteProcessor ip, int minTube, int maxTube) {
		this.ip = ip;
		this.minTube = (int) (Math.floor(minTube/2)*2+1);
		this.maxTube = ((int) Math.floor(maxTube/2)*2+1);
	}
	
	public IntImVesselness(ShortProcessor ip, int minTube, int maxTube) {
		this.ip = ip;
		this.minTube = (int) (Math.floor(minTube/2)*2+1);
		this.maxTube = ((int) Math.floor(maxTube/2)*2+1);
	}
	
	public IntImVesselness(FloatProcessor ip, int minTube, int maxTube) {
		this.ip = ip;
		this.minTube = (int) (Math.floor(minTube/2)*2+1);
		this.maxTube = ((int) Math.floor(maxTube/2)*2+1);
	}
	
	public final void calculate(float Rthresh, float Sthresh) {
		complete = false;
		
		w = ip.getWidth();
		h = ip.getHeight();
		
		calculatedValues = IJ.createHyperStack("Calculated Values", w, h, 1, maxTube - minTube + 1, 4, 32);
		
		for (int i=minTube; i<=(maxTube); i=i+2) {
			
			IntIm2D dX2 = IntIm2D.create(ip.duplicate(), "dx^2");
			IntIm2D dY2 = IntIm2D.create(ip.duplicate(), "dy^2");
			IntIm2D dXdY = IntIm2D.create(ip.duplicate(), "dxdy");
			
			dX2.calculate(i);
			dY2.calculate(i);
			dXdY.calculate(i);
			
			FloatProcessor dX2ip = dX2.createFloat();
			FloatProcessor dY2ip = dY2.createFloat();
			FloatProcessor dXdYip = dY2.createFloat();
			
			float[] dX2pix = (float[]) dX2ip.getPixels();
			float[] dY2pix = (float[]) dY2ip.getPixels();
			float[] dXdYpix = (float[]) dXdYip.getPixels();
			
			double trace;
			double determinant;
			double lambdaAdd;
			double lambdaSub;
			double Rtube;
			double Sbright;
			double gap;
			double xxWeight = 0;
			double xyWeight = 0;
			double fsize = i/2;
			double fsizeSquare = Math.pow(fsize, 2);
			double fsizeQuart = Math.pow(fsizeSquare, 2);
			double xSquare;
			double ySquare;
			double xxGauss;
			double xyGauss;
			double expGauss;
			int gaussRange = (3*i-1)/2;

			float[] lambda1pix = new float[w*h];
			float[] lambda2pix = new float[w*h];
			float[] vesselRankpix = new float[w*h];
			float[] lambda2Signpix = new float[w*h];
			
			for (int x=-gaussRange; x<=gaussRange; x++) {
				xSquare = Math.pow(x, 2);
				for (int y=-gaussRange; y<=gaussRange; y++) {
					ySquare = Math.pow(y, 2);
					expGauss = Math.exp(-(xSquare+ySquare)/(2*fsizeSquare));
					
					xxGauss = (-1 + xSquare/fsizeSquare)*expGauss;
					xxWeight += Math.pow(xxGauss, 2);
					
					xyGauss = (x*y)/(fsizeSquare)*expGauss;
					xyWeight += Math.pow(xyGauss, 2);
				}
			}
			
			xxWeight = Math.sqrt(xxWeight);
			xyWeight = Math.sqrt(xyWeight);
			
			xyWeight = (xyWeight/(3*i-1));
			xxWeight = xxWeight/(3*i*Math.sqrt(2));
			
			IJ.log(Double.toString(xxWeight));
			IJ.log(Double.toString(xyWeight));
			
			for (int j = 0; j<w*h; j++) {
				trace = xxWeight*(dX2pix[j] + dY2pix[j]);
				determinant = (xxWeight*xxWeight*dX2pix[j]*dY2pix[j] - Math.pow(xyWeight*dXdYpix[j],2));
				
				gap =  Math.sqrt(Math.pow(trace, 2) - 4*determinant);
				lambdaAdd = ((trace + gap)/2);
				lambdaSub = ((trace - gap)/2);
				
				if (Math.abs(lambdaAdd)<Math.abs(lambdaSub)) {
					lambda1pix[j] = (float) lambdaAdd;
					lambda2pix[j] = (float) lambdaSub;
				} else {
					lambda1pix[j] = (float) lambdaSub;
					lambda2pix[j] = (float) lambdaAdd;
				}
				
				Rtube = (float) Math.exp(-Math.abs(lambda1pix[j]/lambda2pix[j])/(2*Math.pow(Rthresh,2)));
				Sbright = (float) (1 - Math.exp(-Math.sqrt(Math.pow(lambda1pix[j], 2) + Math.pow(lambda2pix[j], 2))/(2*Math.pow(Sthresh, 2))));
				
				vesselRankpix[j] = (float) (Rtube*Sbright);
				lambda2Signpix[j] = Math.signum(lambda2pix[j]);
			}
			
			calculatedValues.setPosition(1, i-minTube+1, 1);
			calculatedValues.setTitle("Lambda 1 values for vessel size " + i + " pixels");
			calculatedValues.getProcessor().setPixels(lambda1pix);
			
			calculatedValues.setPosition(1, i-minTube+1, 2);
			calculatedValues.setTitle("Lambda 2 for vessel size " + i + " pixels");
			calculatedValues.getProcessor().setPixels(lambda2pix);
			
			calculatedValues.setPosition(1, i-minTube+1, 3);
			calculatedValues.setTitle("Sign of lambda 2 for vessel size " + i + " pixels");
			calculatedValues.getProcessor().setPixels(lambda2Signpix);
			
			calculatedValues.setPosition(1, i-minTube+1, 4);
			calculatedValues.setTitle("Vesselness ranking for vessel size " + i + " pixels");
			calculatedValues.getProcessor().setPixels(vesselRankpix);
		}
		complete = true;
	}
	
	public ImagePlus getMaxVesselValues(float brightOrDark) {
		if (w==0) {
			IJ.error("Vesselness rankings were not calculated. Cannot perform operation.");
		}
		
		vMaxImg = IJ.createHyperStack("Maximum Vesselness", w, h, 1, 2, 1, 32);
		
		vMaxImg.setPosition(1, 1, 1);
		vMaxImg.setTitle("Maximum Vesselness Rank");
		vMaxImg.getProcessor().set(0);
		float[] vesselMax = (float[]) vMaxImg.getProcessor().getPixels();

		vMaxImg.setPosition(1, 2, 1);
		vMaxImg.setTitle("Vessel Size");
		vMaxImg.getProcessor().set(0);
		float[] vesselSize = (float[]) vMaxImg.getProcessor().getPixels();
		
		for (int i = minTube; i<=maxTube; i++) {
			calculatedValues.setPosition(1, i-minTube+1, 4);
			float[] tubeVals = (float[]) calculatedValues.getProcessor().getPixels();
			
			calculatedValues.setPosition(1, i-minTube+1, 3);
			float[] tubeSigns = (float[]) calculatedValues.getProcessor().getPixels();
			
			for (int j = 0; j<w*h; j++) {
				if (tubeVals[j]>vesselMax[j] && (tubeSigns[j]==brightOrDark || brightOrDark==0)) {
					vesselSize[j] = (float) i;
					vesselMax[j] = tubeVals[j];
				}
			}
		}
		
		vMaxImg.setPosition(1, 1, 1);
		vMaxImg.getProcessor().setPixels(vesselMax);
		
		vMaxImg.setPosition(1, 2, 1);
		vMaxImg.getProcessor().setPixels(vesselSize);
		
		return vMaxImg;
	}
	
	public FloatProcessor createVesselFiltImage(float brightOrDark) {
		if (vMaxImg==null) {
			this.getMaxVesselValues(brightOrDark);
		}
		
		vMaxImg.setPosition(1, 1, 1);
		float[] vesselMax = (float[]) vMaxImg.getProcessor().getPixels();
		pip = ip.convertToFloat();
		
		float[] iPix = (float[]) pip.getPixels();
		
		for (int i = 0; i<w*h; i++) {
			iPix[i] = iPix[i] * vesselMax[i];
		}
		
		FloatProcessor vesselImg = new FloatProcessor(w,h,iPix);
		
		return vesselImg;
	}
	
	public static final IntImVesselness create(ImageProcessor ip, int minTube, int maxTube) {
		if (FloatProcessor.class.isInstance(ip))
			return new IntImVesselness((FloatProcessor)ip, minTube, maxTube);
		if (ByteProcessor.class.isInstance(ip))
			return new IntImVesselness((ByteProcessor)ip, minTube, maxTube);
		if (ShortProcessor.class.isInstance(ip))
			return new IntImVesselness((ShortProcessor)ip, minTube, maxTube);
		if (ColorProcessor.class.isInstance(ip)) {
			return new IntImVesselness((ColorProcessor)ip, minTube, maxTube);
		}
		return null;
	}
	
	public final boolean isComplete() {
		return complete;
	}
	
	public final FloatProcessor getVesselSize() {
		vMaxImg.setPosition(1,2,1);
		return vMaxImg.getProcessor().convertToFloatProcessor();
	}
	
	public final FloatProcessor getVesselRank() {
		vMaxImg.setPosition(1,1,1);
		return vMaxImg.getProcessor().convertToFloatProcessor();
	}
	
}

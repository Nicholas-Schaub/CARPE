package nist.ij.quantitativeabsorption;

import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.util.ArrayList;

public class AbsorptiveEntropy {
	private final int iWidth;
	private final int iHeight;
	private final float pixelNum;
	private final float zeroEntropy;
	private float entropy;
	private float averageAbsorbance = 0;
	private float summedTransmittance = 0;
	private float significance = 0.000001F;
	private ArrayList<float[]> absAndTransPix;
	private ImageProcessor absorbanceImage = null;
	private ImageProcessor transmittanceImage = null;
	private ImageProcessor foregroundImage = null;
	private ImageProcessor backgroundImage = null;
	private boolean calculatedAbsorbance = false;
	private final boolean isAbsorbance;
	private final boolean isTransmittance;
	
	public AbsorptiveEntropy(ImageProcessor ip, float pixelSize, boolean isAbsorbance, boolean isTransmittance) {
		this.isAbsorbance = isAbsorbance;
		this.isTransmittance = isTransmittance;
		float[] bpix = (float[]) ip.convertToFloat().getPixels(); 
		pixelNum = bpix.length;
		zeroEntropy = (float) Math.log10(pixelNum);
		iWidth = ip.getWidth();
		iHeight = ip.getHeight();
		
		if (isAbsorbance) {
			absorbanceImage = ip;
		} else if (isTransmittance) {
			transmittanceImage = ip;
		} else {
			foregroundImage = ip;
		}
	}
	
	public float calculateEntropy() {
		
		if (isAbsorbance || isTransmittance || calculatedAbsorbance) {
			if (isAbsorbance && !calculatedAbsorbance) {
				float[] apix = (float[]) absorbanceImage.convertToFloat().getPixels();
				absAndTransPix = getTransformedSigValues(apix,true);
			} else if (isTransmittance && !calculatedAbsorbance) {
				float[] tpix = (float[]) transmittanceImage.convertToFloat().getPixels();
				absAndTransPix = getTransformedSigValues(tpix,false);
			}
			calculatedAbsorbance = true;
		} else {
			return 0;
		}
		
		float[] apix = absAndTransPix.get(0);
		float[] tpix = absAndTransPix.get(1);
		
		averageAbsorbance = 0;
		summedTransmittance = 0;
		
		for (int i=0; i<pixelNum; i++) {
			averageAbsorbance += apix[i]/pixelNum;
			summedTransmittance += tpix[i];
		}
		
		entropy = averageAbsorbance + (float) Math.log10(summedTransmittance) - (float) Math.log10(pixelNum);
		
		return entropy;
	}
	
	public float getZeroEntropy() {return zeroEntropy;}
	
	public float getSignificance() {return significance;}
	
	public void setSignificance(float significance) {
		this.significance = significance;
		calculatedAbsorbance = false;
		if (!isAbsorbance && !isTransmittance && (backgroundImage!=null)) {
			setBackground(backgroundImage);
		}
	}

	public FloatProcessor getAbsorbanceImage() {
		if (isAbsorbance || isTransmittance || calculatedAbsorbance) {
			if (isAbsorbance && !calculatedAbsorbance) {
				absAndTransPix = getTransformedSigValues((float[]) absorbanceImage.getPixels(),true);
			} else if (isTransmittance && !calculatedAbsorbance) {
				absAndTransPix = getTransformedSigValues((float[]) transmittanceImage.getPixels(),false);
			}
			
			calculatedAbsorbance = true;
			return new FloatProcessor(iWidth,iHeight,absAndTransPix.get(0));
		}
		
		return null;
	}
	
	public FloatProcessor getTransmittanceImage() {
		if (isAbsorbance || isTransmittance || calculatedAbsorbance) {
			if (isAbsorbance && !calculatedAbsorbance) {
				absAndTransPix = getTransformedSigValues((float[]) absorbanceImage.getPixels(),true);
			} else if (isTransmittance && !calculatedAbsorbance) {
				absAndTransPix = getTransformedSigValues((float[]) transmittanceImage.getPixels(),false);
			}
			
			calculatedAbsorbance = true;
			return new FloatProcessor(iWidth,iHeight,absAndTransPix.get(1));
		}
		
		return null;
	}
	
	public void setBackground(ImageProcessor ip) {
		float[] bpix = (float[]) ip.getPixelsCopy();
		float bPixelNum = bpix.length;
		
		if (bPixelNum!=pixelNum) {
			calculatedAbsorbance = false;
			return;
		} else if (foregroundImage == null) {
			calculatedAbsorbance = false;
			return;
		}
		
		averageAbsorbance = 0;
		summedTransmittance = 0;
		
		float[] fpix = (float[]) foregroundImage.getPixels();
		float[] tpix = (float[]) transmittanceImage.getPixels();
		
		for (int i=0; i<pixelNum; i++) {
			tpix[i] = fpix[i]/bpix[i];
		}
		
		absAndTransPix = getTransformedSigValues(tpix,false);
		
		calculatedAbsorbance = true;
	}
	
	public ArrayList<float[]> getTransformedSigValues(float[] pixData, boolean isAbsorbance) {
		ArrayList<float[]> absAndTrans = new ArrayList<float[]>();
		float[] transmittance = new float[pixData.length];
		
		if (isAbsorbance) {
			for (int i=0; i<pixData.length; i++) {
				pixData[i] = Math.round(pixData[i]/significance)*significance;
				transmittance[i] = (float) Math.pow(10, -pixData[i]);
			}
		} else {
			for (int i=0; i<pixData.length; i++) {
				pixData[i] = Math.round(-Math.log10(pixData[i])/significance)*significance;
				transmittance[i] = (float) Math.pow(10, -pixData[i]);
			}
		}
		
		absAndTrans.add(pixData);
		absAndTrans.add(transmittance);
		
		return absAndTrans;
	}
	
}

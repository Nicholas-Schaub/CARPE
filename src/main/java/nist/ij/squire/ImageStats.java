package nist.ij.squire;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;

//This class provides utilities for calculating absorption values from bright field images.

// Updated NJS 2015-12-02
// Will now save all raw images if specified in the Control Panel.
public class ImageStats {

	// Images and stacks associated with pixel statistics
	private ImagePlus meanImage;
	private ImagePlus stdImage;
	private ImagePlus absImage;
	private ImagePlus errImage;
	public ImagePlus rawImage;
	
	// Basic image and capture settings.
	public String name;
	public String channelLabel;
	public int width;
	public int height;
	public int bitdepth;
	public int imagebitdepth;
	public int nFrames;
	public int nSlices;
	
	// Properties related to images captured for absorption
	double rSqr;
	int minimumPixInt;
	double bestExp;
	double bestExpIntensity;
	int numExp;

	// Call this function to perform statistics on an ImagePlus object.
	public ImageStats(ImagePlus imp) {
		name = imp.getTitle();
		channelLabel = "";

		rawImage = imp;
		
		// Core and image attributes
		width = (int) imp.getWidth();
		height = (int) imp.getHeight();
		bitdepth = (int) imp.getBitDepth();

		// Make sure the bit depth is something ImageJ can handle
		if ((bitdepth == 12) || (bitdepth==14)) {imagebitdepth=16;} else {imagebitdepth=bitdepth;}

		nFrames = rawImage.getNFrames();
		nSlices = rawImage.getNSlices();
	}
	
	public ImageStats(ImagePlus meanImage, ImagePlus stdImage) {
		name = meanImage.getTitle();
		channelLabel = "";

		rawImage = meanImage;
		this.meanImage = meanImage;
		this.stdImage = stdImage;
		
		// Core and image attributes
		width = (int) meanImage.getWidth();
		height = (int) meanImage.getHeight();
		bitdepth = (int) meanImage.getBitDepth();

		// Make sure the bit depth is something ImageJ can handle
		if ((bitdepth == 12) || (bitdepth==14)) {imagebitdepth=16;} else {imagebitdepth=bitdepth;}

		nFrames = 1;
		nSlices = 1;
	}
	
	private float getError(float sampSTD, float sampInt, float sampNum, float forSTD, float forInt) {
		double ln = Math.log(10);
		double z = 1.96;
		double sigmaI = Math.pow(sampSTD/(ln*sampInt), 2);
		double sigmaIo = Math.pow(forSTD/(ln*forInt), 2);
		double sigmaA = Math.sqrt(sigmaI + sigmaIo);
		float error = (float) (z*sigmaA/Math.sqrt(sampNum));
		
		return error;
	}
	
	public ImagePlus getAbsorbance(ImageStats foreground, ImageStats background) {
		/*****************************************************************************************
		 * This method calculates the absorbance values for the image stored in an instance of
		 * this class.
		 * 
		 * foreground - ImageStats object for a blank image with the light on
		 * background - ImageStats object for a blank image with the light off
		 *****************************************************************************************/
		
		ImagePlus sampSTDImage = this.getFrameDeviation();
		ImagePlus sampMeanImage = this.getFrameMean();
		ImagePlus forSTDImage = foreground.getFrameDeviation();
		ImagePlus forMeanImage = foreground.getFrameMean();
		ImagePlus backMeanImage = background.getFrameMean();

		FloatProcessor absProc = new FloatProcessor(width,height);
		FloatProcessor errProc = new FloatProcessor(width,height);
		int flen = foreground.width*foreground.height;
		float sampSTD;
		float sampInt;
		float forSTD;
		float forInt;
		float backMean;
		float error;
		float abs;
		float[] adjust = new float[nFrames];
		for (int i = 0; i<nFrames; i++) {
			adjust[i] = (float) Math.pow(2, -i);
		}

		for (int j=0; j<flen; j++) {
			for (int frame = 1; frame<=nFrames; frame++) {
				sampMeanImage.setSlice(frame);
				sampSTDImage.setSlice(frame);
				backMean = backMeanImage.getProcessor().getf(j);
				sampSTD = adjust[frame-1]*sampSTDImage.getProcessor().getf(j);
				sampInt = (adjust[frame-1]*sampMeanImage.getProcessor().getf(j)-backMean);
				forSTD = forSTDImage.getProcessor().getf(j);
				forInt = forMeanImage.getProcessor().getf(j)-backMean;
				error = this.getError(sampSTD, sampInt, nSlices, forSTD, forInt);
				
				if (error>0.01 && frame!=nFrames) {
					continue;
				} else {
					abs = (float) -Math.log10(sampInt/(forInt));
					absProc.setf(j, abs);
					errProc.setf(j, error);
					break;
				}
			}
		}
		
		errImage = new ImagePlus(name + " Error",errProc);
		absImage = new ImagePlus(name + " Absorbance",absProc);

		return absImage;
	}
	
	public ImagePlus getErrorImage() {
		return errImage;
	}
	
	private ImagePlus getFrameDeviationAndMean(ImagePlus imp) {
		
		if (stdImage!=null && stdImage.getNFrames()==nFrames) {
			return stdImage;
		}

		stdImage = new ImagePlus();
		int frames = imp.getNFrames();
		int replicates = imp.getNSlices();
		meanImage = IJ.createImage("", width, height, frames, 32);
		ImageStack meanStack = new ImageStack(width,height);
		ImageStack stdStack = new ImageStack(width,height);
		
		int flen = width*height;

		for (int i=1; i<=frames; i++) {
			float[] tpixel = new float[flen];
			float[] fpixelmean = new float[flen];
			double[] dpixelmean = new double[flen];
			float[] fpixeldeviation = new float[flen];
			double[] dpixeldeviation = new double[flen];
			for (int j=1; j<=(replicates); j++) { //loop to calculate the mean
				imp.setPosition(1,j,i);
				tpixel = (float[]) imp.getProcessor().convertToFloat().getPixels();
				for (int k=0; k<flen; k++) {
					dpixeldeviation[k] += tpixel[k]*tpixel[k];
					dpixelmean[k] += tpixel[k];
				}
			}

			for (int j = 0; j<flen; j++) {
				dpixelmean[j] /= (double) replicates;
				dpixeldeviation[j] /= (double) replicates;
				dpixeldeviation[j] = Math.sqrt(dpixeldeviation[j] - dpixelmean[j]*dpixelmean[j]);
				fpixelmean[j] = (float) dpixelmean[j];
				fpixeldeviation[j] = (float) dpixeldeviation[j];
			}
			stdStack.addSlice(imp.getImageStack().getSliceLabel(imp.getCurrentSlice()),
					new FloatProcessor(width,height,fpixeldeviation),
					i-1);
			
			meanStack.addSlice(imp.getImageStack().getSliceLabel(imp.getCurrentSlice()),
					new FloatProcessor(width,height,fpixelmean),
					i-1);
		}
		
		stdImage = new ImagePlus(name,stdStack);
		meanImage = new ImagePlus(name,meanStack);
		
		return stdImage;
	}
	
	public ImagePlus getFrameDeviation() {
		if (stdImage!=null) {
			return stdImage;
		}
		return getFrameDeviationAndMean(rawImage);
	}

	public ImagePlus getFrameMean() {
		if (meanImage!=null) {
			return meanImage;
		}
		getFrameDeviationAndMean(rawImage);
		return meanImage;
	}

	public String getName() {return name;}

}
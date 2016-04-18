package nist.ij.plugins;

import ij.IJ;
import ij.ImagePlus;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import nist.ij.quantitativeabsorption.AbsorptiveEntropy;

public class AbsorptiveEntropyPlugin implements PlugIn {
	public ImagePlus imp;
	public ImageProcessor ip;
	public AbsorptiveEntropy absE;
	public float entropy;
	public float pixelLength;
	public float zeroEntropy;
	public float numPixels;
	public static ResultsTable absorptiveEntropyTable = new ResultsTable();
	
	@Override
	public void run(String arg) {
		int resultsCounter = absorptiveEntropyTable.getCounter();
		if (resultsCounter==0) {
			tableSetup();
		}
		
		imp = IJ.getImage();
		ip = imp.getProcessor();
		pixelLength = (float) imp.getCalibration().pixelWidth;
		numPixels = ip.getPixelCount();
		
		if (arg.equals("absorbance")) {
			absE = new AbsorptiveEntropy(ip,pixelLength,true,false);
		} else if (arg.equals("transmittance")) {
			absE = new AbsorptiveEntropy(ip,pixelLength,false,true);
		}
		
		entropy = absE.calculateEntropy();
		zeroEntropy = absE.getZeroEntropy();

		absorptiveEntropyTable.incrementCounter();
		
		addResult();
		absorptiveEntropyTable.show("Entropy Results");
	}
	
	private void tableSetup() {
		absorptiveEntropyTable.getFreeColumn("Pixel Length");
		absorptiveEntropyTable.setDecimalPlaces(0, 3);
		absorptiveEntropyTable.getFreeColumn("Number of Pixels");
		absorptiveEntropyTable.setDecimalPlaces(1, 0);
		absorptiveEntropyTable.getFreeColumn("Area Imaged");
		absorptiveEntropyTable.setDecimalPlaces(2, 3);
		absorptiveEntropyTable.getFreeColumn("Zero Entropy");
		absorptiveEntropyTable.setDecimalPlaces(3, 2);
		absorptiveEntropyTable.getFreeColumn("Entropy");
		absorptiveEntropyTable.setDecimalPlaces(4, 3);
	}
	
	private void addResult() {
		absorptiveEntropyTable.addLabel(imp.getTitle());
		absorptiveEntropyTable.addValue("Pixel Length",pixelLength);
		absorptiveEntropyTable.addValue("Number of Pixels",numPixels);
		absorptiveEntropyTable.addValue("Area Imaged",Math.pow(pixelLength, 2)*numPixels);
		absorptiveEntropyTable.addValue("Zero Entropy",zeroEntropy);
		absorptiveEntropyTable.addValue("Entropy", entropy);
	}

}

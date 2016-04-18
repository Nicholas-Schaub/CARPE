package nist.ij.plugins;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.awt.AWTEvent;

public class FastVesselnessPlugin implements ExtendedPlugInFilter, DialogListener {
	protected ImagePlus image;
	protected static int minTube = 5;
	protected static int maxTube = 10;
	protected static float brightOrDark;
	protected static String direction = "dx^2";
	protected static boolean createFloat = false;
	protected static boolean getRank = false;
	protected static boolean getSize = false;
	protected static String imageName = "";
	protected boolean wasOKed = false;
	protected static float Rthresh = 2F;
	protected static float Sthresh = 100F;

	protected nist.ij.integralimage.IntImVesselness intImV = null;
	protected ImageProcessor pip = null;
	protected static final int flags = 29;
	
	@Override
	public int setup(String arg, ImagePlus imp) {
		// TODO Auto-generated method stub
		return flags;
	}

	@Override
	public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
		String[] derivTypes = new String[3];
		derivTypes[0] = "Bright";
		derivTypes[1] = "Dark";
		derivTypes[2] = "Bright or Dark";
		
		GenericDialog gd = new GenericDialog("Fast Vesselness (Frangi)");
		
		gd.addNumericField("Minimum Tube Size: ", minTube, 0, 6, "pixels");
		gd.addNumericField("Maximum Tube Size: ", maxTube, 0, 6, "pixels");
		gd.addNumericField("R Threshold (tube like): ", Rthresh, 1);
		gd.addNumericField("S Threshold (tube intensity): ", Sthresh, 1);
		gd.addChoice("Bright or dark objects? ", derivTypes, derivTypes[0]);
		gd.addCheckbox("Get Vesselness Rank ", getRank);
		gd.addCheckbox("Get Vessel Sizes ", getSize);
		gd.addCheckbox("Create 32-bit float", createFloat);
		gd.addPreviewCheckbox(pfr);
		gd.addDialogListener(this);
		
		imageName = imp.getTitle();
		pip = imp.getProcessor();
		intImV = nist.ij.integralimage.IntImVesselness.create(pip, minTube, maxTube);

		gd.showDialog();
		if (gd.wasCanceled()) return 4096;

		IJ.register(getClass());
		
		return IJ.setupDialog(imp, 29);
	}
	
	@Override
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
		minTube = (int) gd.getNextNumber();
		maxTube = (int) gd.getNextNumber();
		Rthresh = (float) gd.getNextNumber();
		Sthresh = (float) gd.getNextNumber();
		
		String brightOrDarkSelect = gd.getNextChoice();
		if (brightOrDarkSelect == "Bright") {
			brightOrDark = -1;
		} else if (brightOrDarkSelect =="Dark") {
			brightOrDark = 1;
		} else {
			brightOrDark = 0;
		}
		
		getRank = gd.getNextBoolean();
		getSize = gd.getNextBoolean();
		createFloat = gd.getNextBoolean();

		wasOKed = gd.wasOKed();
		
		if (gd.invalidNumber()) {
			return false;
		}
		return true;
	}
	
	@Override
	public void run(ImageProcessor ip) {
		pip = ip;
		
		intImV = nist.ij.integralimage.IntImVesselness.create(pip, minTube, maxTube);
		
		intImV.calculate(Rthresh, Sthresh);
		
		if (wasOKed) {
			if (createFloat) {
				new ImagePlus(imageName + " - Vessel Filtered", intImV.createVesselFiltImage(brightOrDark)).show();
			}
			if (getRank) {
				new ImagePlus(imageName + " - Vessel Filtered", intImV.createVesselFiltImage(brightOrDark)).show();
				new ImagePlus(imageName + " - Vessel Rank", intImV.getVesselRank()).show();
			}
			if (getSize) {
				new ImagePlus(imageName + " - Vessel Filtered", intImV.createVesselFiltImage(brightOrDark)).show();
				new ImagePlus(imageName + " - Vessel Sizes", intImV.getVesselSize()).show();
			}
		} else if (ip instanceof ByteProcessor) {
			ip.setPixels(intImV.createVesselFiltImage(brightOrDark).convertToByte(false).getPixels());
		} else if (ip instanceof ShortProcessor) {
			ip.setPixels(intImV.createVesselFiltImage(brightOrDark).convertToShort(false).getPixels());
		} else {
			ip.setPixels(intImV.createVesselFiltImage(brightOrDark).getPixels());
		}
	}

	@Override
	public void setNPasses(int nPasses) {
		// TODO Auto-generated method stub
		
	}

}

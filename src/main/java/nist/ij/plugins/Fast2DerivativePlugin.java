/*
 * To the extent possible under law, the Fiji developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */
package nist.ij.plugins;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.process.ImageProcessor;

import java.awt.AWTEvent;

public class Fast2DerivativePlugin implements ExtendedPlugInFilter, DialogListener {
	protected ImagePlus image;
	protected static int blockSize = 10;
	protected static String direction = "dx^2";
	protected static boolean createFloat = false;
	protected static String imageName = "";
	protected boolean wasOKed = false;

	protected nist.ij.integralimage.IntIm2D int2D = null;
	protected ImageProcessor pip = null;
	protected static final int flags = 29;

	public int setup(String arg, ImagePlus imp) {
		return flags;
	}
	
	public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
		String[] derivTypes = new String[3];
		derivTypes[0] = "dx^2";
		derivTypes[1] = "dy^2";
		derivTypes[2] = "dxdy";
		
		GenericDialog gd = new GenericDialog("Fast 2nd Derivative Approximation");
		
		gd.addNumericField("Block Size: ", blockSize, 0, 6, "pixels");
		gd.addChoice("Derivative Type:", derivTypes, derivTypes[0]);
		gd.addCheckbox("Create 32-bit float", createFloat);
		gd.addPreviewCheckbox(pfr);
		gd.addDialogListener(this);
		
		imageName = imp.getTitle();
		pip = imp.getProcessor();
		int2D = nist.ij.integralimage.IntIm2D.create(pip, direction);

		gd.showDialog();
		if (gd.wasCanceled()) return 4096;

		IJ.register(getClass());
		
		return IJ.setupDialog(imp, 29);
	}
	
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
		blockSize = (int) gd.getNextNumber();
		direction = gd.getNextChoice();
		createFloat = gd.getNextBoolean();

		wasOKed = gd.wasOKed();
		
		if (gd.invalidNumber()) {
			return false;
		}
		return true;
	}
	
	/**
	 * @see ij.plugin.filter.PlugInFilter#run(ij.process.ImageProcessor)
	 */
	@Override
	public void run(ImageProcessor ip) {
		pip = ip;
		
		int2D = nist.ij.integralimage.IntIm2D.create(pip, direction);
		
		synchronized (this) {
			
		}
		
		int2D.calculate(blockSize);
		
		if (createFloat && wasOKed) {
			new ImagePlus(imageName + " " + direction,int2D.createFloat()).show();
		}
		
	}

	@Override
	public void setNPasses(int nPasses) {}

}

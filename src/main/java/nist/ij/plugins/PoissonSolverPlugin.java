package nist.ij.plugins;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.process.ImageProcessor;

import java.awt.AWTEvent;

public class PoissonSolverPlugin implements ExtendedPlugInFilter, DialogListener {
	protected ImagePlus image;
	protected static int blockSize = 10;
	protected static String direction = "dx^2";
	protected static boolean createFloat = false;
	protected static String imageName = "";
	protected boolean wasOKed = false;

	protected nist.ij.poissonsolver.Poisson2D poiSolver = null;
	protected ImageProcessor pip = null;
	protected static final int flags = 29;
	
	@Override
	public int setup(String arg, ImagePlus imp) {
		return flags;
	}
	
	@Override
	public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
		GenericDialog gd = new GenericDialog("Poisson Solver");
		
		gd.addPreviewCheckbox(pfr);
		
		imageName = imp.getTitle();
		pip = imp.getProcessor().convertToFloatProcessor();
		poiSolver = new nist.ij.poissonsolver.Poisson2D(pip);
		
		gd.showDialog();
		if (gd.wasCanceled()) return 4096;

		IJ.register(getClass());
		
		return IJ.setupDialog(imp, 29);
	}
	
	@Override
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public void run(ImageProcessor ip) {
		// TODO Auto-generated method stub
		pip = ip;
		
		poiSolver = new nist.ij.poissonsolver.Poisson2D(pip);
		
		poiSolver.calculate();
		
		new ImagePlus("", poiSolver.getProcessor()).show();
	}
	
	@Override
	public void setNPasses(int nPasses) {
		// TODO Auto-generated method stub
		
	}
	
	public static void main(String[] args) {
		// set the plugins.dir property to make the plugin appear in the Plugins menu
		Class<?> clazz = PoissonSolverPlugin.class;
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
		System.setProperty("plugins.dir", pluginsDir);

		// start ImageJ
		new ImageJ();

		// open the Clown sample
		ImagePlus image = IJ.openImage("C:\\Program Files\\Micro-Manager-1.4\\images\\squareclownlaplacian.gif");
		image.show();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	}

}

package nist.ij.plugins;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.process.FloatProcessor;

public class LabelCobblestonePlugin implements PlugIn {
	protected ImagePlus image;
	protected static String imageName = "";

	protected FloatProcessor pip = null;
	protected static final int flags = 29;
	
	public static void main(String[] args) {
		// set the plugins.dir property to make the plugin appear in the Plugins menu
		Class<?> clazz = LabelCobblestonePlugin.class;
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
		System.setProperty("plugins.dir", pluginsDir);

		// start ImageJ
		new ImageJ();

		// open the Clown sample
		ImagePlus image = IJ.openImage("C:\\Program Files\\Micro-Manager-1.4\\images\\Cobblestone.tif");
		image.show();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	}

	@Override
	public void run(String arg) {
		// TODO Auto-generated method stub
		
	}

}

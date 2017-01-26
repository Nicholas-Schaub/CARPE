package nist.ij.plugins;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.measure.CurveFitter;
import ij.plugin.PlugIn;
import ij.process.FloatProcessor;
import nist.ij.guitools.FileChooserPanel;
import nist.ij.guitools.TextFieldInputPanel;

public class RemoveScatter implements PlugIn {
	protected ImagePlus image;
	protected static String imageName = "";

	protected FloatProcessor pip = null;
	protected static final int flags = 29;
	Double[] red;
	
	// Dialog components
	JPanel dialog;
	ArrayList<TextFieldInputPanel> wavelengths = new ArrayList<TextFieldInputPanel>();
	ArrayList<FileChooserPanel> images = new ArrayList<FileChooserPanel>();
	ArrayList<JPanel> channels = new ArrayList<JPanel>();
	JButton addChannelButton;
	JButton removeChannelButton;
	JButton goButton;
	
	FileChooserPanel redFilePath;
	FileChooserPanel greenFilePath;
	FileChooserPanel blueFilePath;
	
	public static void main(String[] args) {
		// set the plugins.dir property to make the plugin appear in the Plugins menu
		Class<?> clazz = RemoveScatter.class;
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
		System.setProperty("plugins.dir", pluginsDir);

		// start ImageJ
		new ImageJ();

		// open the Clown sample
		ImagePlus image = IJ.openImage("C:\\Program Files\\Micro-Manager-1.4\\images\\melanin.tif");
		image.show();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	}

	private class goAction implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			// TODO Auto-generated method stub
			long startTime = System.currentTimeMillis();
			image = IJ.getImage();
			pip = image.getProcessor().convertToFloatProcessor();
			int channels = image.getNChannels();
			int width = image.getWidth();
			int height = image.getHeight();
			
			double[] lambda = {(double) 640, (double) 548, (double) 496};
			ArrayList<Double[]> dpix = image2Double(image);
			int imgSize = dpix.get(0).length;
			ExecutorService cfThreads = Executors.newFixedThreadPool(7);
			
			double[] spix = new double[imgSize];
			double[] mpix = new double[imgSize];
			
			for (int i=0; i<imgSize; i++) {
				double[] x = new double[channels];
				double[] y = new double[channels];
				for (int j=0; j<dpix.size(); j++) {
					x[j] = lambda[j];
					y[j] = dpix.get(j)[i];
				}
				cfThreads.execute(new PixReg(x,y,spix,mpix,i));
			}
			
			cfThreads.shutdown();
			try {
				cfThreads.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			FloatProcessor sP = new FloatProcessor(width,height,spix);
			FloatProcessor mP = new FloatProcessor(width,height,mpix);
			
			ImagePlus sI = new ImagePlus("Scatter",sP);
			ImagePlus mI = new ImagePlus("Melanin",mP);
			
			sI.show();
			mI.show();
			long stopTime = System.currentTimeMillis();

			IJ.showProgress(1);
			System.out.println("Time to completion (ms): " + Long.toString(stopTime-startTime));
		}
	}

	public ArrayList<Double[]> image2Double(ImagePlus imp) {
		int channels = imp.getNChannels();
		ArrayList<Double[]> dpix = new ArrayList<Double[]>();
		for (int i=0; i<channels; i++) {
			imp.setPosition(i+1);
			float[] fpix = (float[]) imp.getProcessor().getPixels();
			dpix.add(new Double[fpix.length]);
			for (int j=0; j<fpix.length; j++) {
				dpix.get(i)[j] = (double) fpix[j];
			}
		}
		return dpix;
	}
	
	class PixReg implements Runnable {

		private CurveFitter cf;
		double[] spix;
		double[] mpix;
		int index;
		
		public PixReg(double[] x, double[] y, double[] spix, double[] mpix, int index) {
			cf = new CurveFitter(x,y);
			this.spix = spix;
			this.mpix = mpix;
			this.index = index;
		}

		public void run() {
			if (index%1000==0) {
				System.out.println("Current pixel: " + index);
				IJ.showProgress(index/(double) spix.length);
			}
			String fitEquation = "y = a + b*0.1974*exp(-0.0045*x)";
			double[] initParams = {cf.getYPoints()[2],cf.getYPoints()[2]/(0.1974*Math.exp(-.0045*cf.getXPoints()[2]))};
			cf.doCustomFit(fitEquation, initParams, false);
			spix[index] = cf.getParams()[0];
			mpix[index] = cf.getParams()[1];
		}
		
	}
	
	public void showDialog() {
		// Set up dialog components
		JFrame dialog = new JFrame();
		dialog.setTitle("Quantitative Absorption");
		dialog.setSize(new Dimension(501,251));
		
		JPanel content = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		
		redFilePath = new FileChooserPanel("Red Image (640 nm): ", "");
		greenFilePath = new FileChooserPanel("Green Image (548 nm): ", "");
		blueFilePath = new FileChooserPanel("Green Image (496 nm): ", "");
		
		goButton = new JButton("GO!");
		goButton.setFont(new Font("Arial",Font.BOLD,16));
		goButton.setForeground(new Color(0,201,201));
		
		// Action listeners
		goButton.addActionListener(new goAction());
		
		// Organize and display dialog components
		c.insets = new Insets(2,8,2,8);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		
		c.ipady = 0;
		c.ipadx = 0;
		c.gridwidth = 4;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		content.add(redFilePath,c);
		
		c.gridy++;
		content.add(greenFilePath,c);
		
		c.gridy++;
		content.add(blueFilePath,c);
		
		c.gridy++;
		c.fill = GridBagConstraints.NONE;
		c.ipadx = 10;
		c.ipady = 10;
		content.add(goButton,c);
		content.setPreferredSize(content.getPreferredSize());
		
		dialog.add(content);
		dialog.setLocationRelativeTo(null);
		dialog.setPreferredSize(dialog.getPreferredSize());
		dialog.setVisible(true);
	}

	@Override
	public void run(String arg) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		showDialog();
	}
}

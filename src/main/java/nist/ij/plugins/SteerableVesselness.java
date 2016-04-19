package nist.ij.plugins;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.PlugIn;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import nist.ij.guitools.TextFieldInputPanel;
import nist.ij.guitools.ValidatorDbl;
import nist.ij.guitools.ValidatorInt;
import steerablej.SteerableDetector;

public class SteerableVesselness implements PlugIn {
	String filesep = Pattern.quote(File.separator);
	String fileind = Pattern.quote(".");
	
	// default settings
	protected int minTube = 2;
	protected int maxTube = 7;
	protected double rThresh = 2;
	protected double sThresh = 50;
	protected double[] dxx = {0.113D,-0.392D,0.025D,-0.184D,0.034D};
	protected double[] dx = {-1.1215,-0.5576,-0.018,-0.0415,-0.0038};
	protected int nd = 128;
	
	// image settings
	protected ImagePlus imp;
	protected double[] dpix;
	protected int nx;
	protected int ny;
	protected int nxy;
	protected int nxyd;
	
	// outputs
	protected ImagePlus result;
	
	// Dialog components
	JPanel dialog;
	TextFieldInputPanel minTubeInput;
	TextFieldInputPanel maxTubeInput;
	TextFieldInputPanel rThreshInput;
	TextFieldInputPanel sThreshInput;
	JButton goButton;

	protected static final int flags = 29;
	
	public static void main(String[] args) {
		// set the plugins.dir property to make the plugin appear in the Plugins menu
		Class<?> clazz = AbsorptionImage.class;
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
		System.setProperty("plugins.dir", pluginsDir);

		// start ImageJ
		new ImageJ();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	}
	
	public void showDialog() {
		// Set up dialog components
		JFrame dialog = new JFrame();
		dialog.setTitle("Quantitative Absorption");
		dialog.setSize(new Dimension(501,251));
		
		JPanel content = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		
		minTubeInput = new TextFieldInputPanel("Minimum ridge size: ", Integer.toString(minTube), new ValidatorInt(1,51));
		maxTubeInput = new TextFieldInputPanel("Maximum ridge size: ", Integer.toString(maxTube), new ValidatorInt(1,51));
		rThreshInput = new TextFieldInputPanel("R Threshold (ridgeness): ", Double.toString(rThresh), new ValidatorDbl(0,10));
		sThreshInput = new TextFieldInputPanel("S Threshold (intensity): ", Double.toString(sThresh), new ValidatorDbl(1,1000));
		
		goButton = new JButton("GO!");
		goButton.setFont(new Font("Arial",Font.BOLD,16));
		goButton.setForeground(new Color(0,201,201));
		
		// action listeners
		goButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent paramActionEvent) {
				// image attributes
				imp = IJ.getImage();
				dpix = (double[]) imp.getProcessor().getPixels();
				nx = imp.getWidth();
				ny = imp.getHeight();
				nxy = (nx*ny);
				nxyd = nxy*nd;
				
				// algorithm settings
				minTube = (Integer) minTubeInput.getValue();
				maxTube = (Integer) maxTubeInput.getValue();
				rThresh = (Double) rThreshInput.getValue();
				sThresh = (Double) sThreshInput.getValue();
				
				runFrangi();
			}
		});
		
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
		content.add(minTubeInput,c);
		
		c.gridy++;
		content.add(maxTubeInput,c);
		
		c.gridy++;
		content.add(rThreshInput,c);
		
		c.gridy++;
		content.add(sThreshInput,c);
		
		c.gridy++;
		c.fill = GridBagConstraints.NONE;
		c.ipadx = 10;
		c.ipady = 10;
		content.add(goButton,c);
		
		dialog.add(content);
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
	}
	
	public void runFrangi() {

	}
	
	private ImagePlus getHessian(ImagePlus ddImage, int sigma) {
		SteerableDetector dXX = new SteerableDetector(dpix,nx,ny,1,sigma,4,dxx);
		ImagePlus orientations = double2ImagePlus("",dXX.computeRotations(nd),nx,ny,nd);
		ImagePlus maxProj = getMaxProj(orientations);
		addMinSlice(orientations,maxProj);
	}
	
	private ImagePlus getMaxProj(ImagePlus ddImage) {
		double[] maxVals = new double[2*nxy];
		int stackSize = ddImage.getStackSize();
		
		for (int d=1; d<=stackSize; d++) {
			ddImage.setPosition(d);
			double[] pix = (double[]) ddImage.getProcessor().getPixels();
			for (int i = 0; i<nxy; i++) {
				if (pix[i]>maxVals[i]) {
					maxVals[i] = pix[i];
					maxVals[nxy + i] = d;
				}
			}
		}
		return double2ImagePlus("",maxVals,nx,ny,2);
	}
	
	private void addMinSlice(ImagePlus ddImage, ImagePlus maxProj) {
		maxProj.setSlice(2);
		int[] pos = (int[]) maxProj.getProcessor().getPixels();
		
	}
	
	private ImagePlus double2ImagePlus(String title, double[] pix, int width, int height, int stacks) {
	    ImageStack localImageStack = new ImageStack(width, height);
	    for (int i = 0; i < stacks; i++) {
	    	double[] arrayOfDouble = new double[width * height];
	    	
	    	for (int j = 0; j < width * height; j++) {
	    		arrayOfDouble[j] = pix[(j + i * width * height)];
	    	}
	    	localImageStack.addSlice("", new FloatProcessor(width, height, arrayOfDouble));
	    }
	    return new ImagePlus(title, localImageStack);
	}

	@Override
	public void run(String arg0) {
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

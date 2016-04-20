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
import ijtools.Convolver2D;
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
	protected int ortho = nd/4;
	
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
		Class<?> clazz = SteerableVesselness.class;
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
				dpix = castDoublePixels(imp.getProcessor());
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
		getHessian(imp, 3).show();
	}
	
	private ImagePlus getHessian(ImagePlus ddImage, int sigma) {
		for (double val : dxx) {
			IJ.log(Double.toString(val));
		}
		SteerableDetector dXX = new SteerableDetector(dpix,nx,ny,1,sigma,4,dxx);
		SteerableDetector dX = new SteerableDetector(dpix,nx,ny,1,sigma,5,dx);
		dX.run();
		dXX.run();
		ImagePlus ddOrientations = double2ImagePlus("",dXX.computeRotations(nd),nd);
		ImagePlus dOrientations = double2ImagePlus("",dX.computeRotations(nd),nd);
		ImagePlus hessian = getMaxDeriv(ddOrientations);
		addMinDeriv(ddOrientations,hessian);
		addMixedDeriv(dOrientations,hessian,sigma);
		return hessian;
	}
	
	private ImagePlus getMaxDeriv(ImagePlus ddImage) {
		double[] maxVals = new double[2*nxy];
		int stackSize = ddImage.getStackSize();
		
		for (int d=1; d<=stackSize; d++) {
			ddImage.setPosition(d);
			double[] pix = castDoublePixels(ddImage.getProcessor());
			for (int i = 0; i<nxy; i++) {
				if (pix[i]>maxVals[i]) {
					maxVals[i] = pix[i];
					maxVals[nxy + i] = d;
				}
			}
		}
		ImagePlus maxProj = double2ImagePlus("",maxVals,2);
		maxProj.getStack().setSliceLabel("Maximum 2nd Derivative", 1);
		maxProj.getStack().setSliceLabel("Position", 2);
		return double2ImagePlus("",maxVals,2);
	}
	
	private void addMinDeriv(ImagePlus ddImage, ImagePlus hessian) {
		hessian.setSlice(2);
		int[] pos = castIntPixels(hessian.getProcessor());
		double[] minVals = new double[nxy];
		
		for (int i=0; i<nxy; i++) {
			int index = ((pos[i]+ortho) % nd) + 1;
			int modifier = (int) Math.signum(index-pos[i]);
			ddImage.setPosition(index);
			float[] pix = (float[]) ddImage.getProcessor().getPixels();
			minVals[i] = (double) modifier * (double) pix[i];
		}
		
		hessian.getStack().addSlice("Orthogonal 2nd Derivative", new FloatProcessor(nx,ny,minVals), 1);
	}
	
	private void addMixedDeriv(ImagePlus dImage, ImagePlus hessian, int sigma) {
		int gWidth = 4*sigma + 1;
		double[] g = new double[gWidth];
		double sigma2 = (double) sigma*sigma;
		
		for (int i=0; i<gWidth; i++) {
			g[i] = Math.exp(-(i*i)/(2.0D*sigma2));
		}
		
		double[] fImage = Convolver2D.convolveEvenX(dpix, g, nx, ny);
		fImage = Convolver2D.convolveEvenY(fImage, g, nx, ny);
	}
	
	private ImagePlus double2ImagePlus(String title, double[] pix, int stacks) {
	    ImageStack localImageStack = new ImageStack(nx, ny);
	    for (int i = 0; i < stacks; i++) {
	    	double[] arrayOfDouble = new double[nx * ny];
	    	
	    	for (int j = 0; j < nx * ny; j++) {
	    		arrayOfDouble[j] = pix[(j + i * nx * ny)];
	    	}
	    	localImageStack.addSlice("", new FloatProcessor(nx, ny, arrayOfDouble));
	    }
	    return new ImagePlus(title, localImageStack);
	}
	
	private double[] castDoublePixels(ImageProcessor ip) {
		float[] pix = (float[]) ip.convertToFloatProcessor().getPixels();
		double[] dpix = new double[pix.length];
		int i = 0;
		for (float val: pix) {
			dpix[i++] = (double) val;
		}
		return dpix;
	}
	
	private int[] castIntPixels(ImageProcessor ip) {
		float[] pix = (float[]) ip.convertToFloatProcessor().getPixels();
		int[] ipix = new int[pix.length];
		int i = 0;
		for (float val: pix) {
			ipix[i++] = (int) val;
		}
		return ipix;
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

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
import java.io.FilenameFilter;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import nist.ij.guitools.DirectoryChooserPanel;
import nist.ij.guitools.FileChooserPanel;
import nist.ij.imagestats.ImageStats;

public class AbsorptionImage implements PlugIn {
	String filesep = Pattern.quote(File.separator);
	String fileind = Pattern.quote(".");
	
	// Images
	protected String imageDir;
	protected String outputDir;
	protected String calibFile;
	protected String blankFile;
	protected String fileType;
	
	protected ImagePlus ipCalib;
	protected ImagePlus ipBlank;
	protected ImageStats isCalib;
	protected ImageStats isBlank;
	
	// Dialog components
	JPanel dialog;
	FileChooserPanel calibField;
	FileChooserPanel blankField;
	DirectoryChooserPanel imageField;
	DirectoryChooserPanel saveField;
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

		// open the Clown sample
		ImagePlus image = IJ.openImage("C:\\Program Files\\Micro-Manager-1.4\\images\\melanin.tif");
		image.show();

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
		
		
		calibField = new FileChooserPanel("Calibration Image: ","");
		blankField = new FileChooserPanel("Blank Image: ", "");
		imageField = new DirectoryChooserPanel("Image Directory: ","");
		saveField = new DirectoryChooserPanel("Save Directory","");
		
		goButton = new JButton("GO!");
		goButton.setFont(new Font("Arial",Font.BOLD,16));
		goButton.setForeground(new Color(0,201,201));
		
		// Action listeners
		goButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent paramActionEvent) {
				String[] temp = calibField.getFile().getName().split(filesep);
				calibFile = temp[temp.length-1];
				IJ.log("Calibration File: " + calibFile);
				temp = blankField.getFile().getName().split(filesep);
				blankFile = temp[temp.length-1];
				IJ.log("Blank File: " + blankFile);
				temp = calibFile.split(fileind);
				fileType = temp[temp.length-1];
				IJ.log("File type: " + fileType);
				if (!blankField.getValue().endsWith(fileType)) {
					IJ.error("Blank and Calibration images must be same file type.");
					return;
				}
				ipCalib = IJ.openImage(calibField.getValue());
				ipBlank = IJ.openImage(blankField.getValue());
				isCalib = new ImageStats(ipCalib);
				isBlank = new ImageStats(ipBlank);
				imageDir = imageField.getValue();
				outputDir = saveField.getValue();
				ipCalib.show();
				ipBlank.show();
				IJ.log("Image Directory: " + imageDir);
				IJ.log("Output Directory: " + outputDir);
				File[] images = getFileInDir(imageDir);
				for (int i=0; i<images.length; i++) {
					IJ.log("Image: " + images[i].getName());
				}
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
		content.add(calibField,c);
		
		c.gridy++;
		content.add(blankField,c);
		
		c.gridy++;
		content.add(imageField,c);
		
		c.gridy++;
		content.add(saveField,c);
		
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

	public void processAbsorption() {
		for (int i=0; i<imageDir.length(); i++) {
			
		}
	}
	
	public File[] getFileInDir(String directory) {
		File folder = new File(directory);
		return folder.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File directory, String name) {
				if (name==blankFile || name==calibFile) {
					return false;
				} else {
					return name.toLowerCase().endsWith(fileType);
				}
			}
			
		});
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
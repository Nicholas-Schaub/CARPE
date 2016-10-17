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
	
	// Image directories
	protected String imageDir;
	protected String outputDir;
	protected String foreFile;
	protected String backFile;
	protected String fileType;
	protected File[] images;
	
	// Image classes
	protected ImageStats isFore;
	protected ImageStats isBack;
	
	// Dialog components
	JPanel dialog;
	FileChooserPanel foreField;
	FileChooserPanel backField;
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
		
		foreField = new FileChooserPanel("Foreground Image: ", "");
		backField = new FileChooserPanel("Background Image: ", "");
		imageField = new DirectoryChooserPanel("Image Directory: ","");
		saveField = new DirectoryChooserPanel("Save Directory","");
		
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
		content.add(foreField,c);
		
		c.gridy++;
		content.add(backField,c);
		
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
	
	private class goAction implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent paramActionEvent) {
			// get calibration file, foreground file, and background file
			String[] temp = foreField.getFile().getName().split(filesep);
			foreFile = temp[temp.length-1];
			IJ.log("Foreground File: " + foreFile);
			temp = backField.getFile().getName().split(filesep);
			backFile = temp[temp.length-1];
			IJ.log("Background File: " + backFile);
			String[] strsplit = temp[temp.length-1].split(fileind);
			fileType = strsplit[strsplit.length-1];
			
			isFore = new ImageStats(IJ.openImage(foreField.getValue()));
			
			isBack = new ImageStats(IJ.openImage(backField.getValue()));
			
			// get the directory with images, and find all images with same file type as foreground
			imageDir = imageField.getValue();
			outputDir = saveField.getValue();
			images = getFileInDir(imageDir);
			System.out.println(images.length);
			processAbsorption();
		}
		
	}

	public void processAbsorption() {
		for (int i=0; i<images.length; i++) {
			System.out.println(images[i]);
			ImageStats currentFile = new ImageStats(IJ.openImage(images[i].getAbsolutePath()));
			ImagePlus absorbance = currentFile.getAbsorbance(isFore, isBack);
			IJ.saveAsTiff(absorbance, outputDir+File.separator+images[i].getName());
			IJ.saveAsTiff(currentFile.getErrorImage(), outputDir+File.separator+images[i].getName()+" Error");
			IJ.log("Saved image: " + images[i].getName());
		}
	}
	
	public File[] getFileInDir(String directory) {
		File folder = new File(directory);
		return folder.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File directory, String name) {
				if (name==foreFile || name==backFile) {
					return false;
				} else {
					System.out.println(name);
					return name.toLowerCase().endsWith(fileType);
				}
			}
			
		});
	}
	
	// Action listener for GO button.

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
package nist.ij.plugins;

import java.awt.BorderLayout;
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

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.ProgressMonitor;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import nist.ij.guitools.DirectoryChooserPanel;
import nist.ij.guitools.FileChooserPanel;
import nist.ij.squire.ImageStats;
import nist.ij.squire.SquireFileSystem;

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
	
	/* Dialog components */
	// main panels
	JPanel dialog;
	JPanel squirePanel;
	JPanel manualPanel;
	
	// select files, or use SQuIRE file system.
	private JToggleButton useSQuIREFiles;
	private JToggleButton selectFiles;
	private ButtonGroup fileSystemGroup;
	
	// GUI components to use SQuIRE file system
	private FileChooserPanel squireFolder;
	private ButtonGroup folderDepth;
	private JToggleButton singleChannel;
	private JToggleButton singleTimePoint;
	private JToggleButton allTimePoints;
	private JToggleButton allSamples;
	
	// GUI components for select files - no necessarily attached to SQuIRE file system
	private FileChooserPanel foreField;
	private FileChooserPanel backField;
	private DirectoryChooserPanel imageField;
	private DirectoryChooserPanel saveField;
	
	// Options checkbox
	private JCheckBox skipProcessedBox;
	private JCheckBox calculateErrorBox;
	
	// calculate absorption values
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
		//ImagePlus image = IJ.openImage("C:\\Program Files\\Micro-Manager-1.4\\images\\melanin.tif");
		//image.show();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	}
	
	public void showDialog() {
		initElements();
		
		// Set up dialog components
		JFrame dialog = new JFrame();
		dialog.setTitle("Quantitative Absorption");
		dialog.setSize(new Dimension(501,283));
		
		JPanel content = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		
		// Basic constraints
		c.insets = new Insets(1, 1, 1, 1);
		c.anchor = GridBagConstraints.NORTH;
		c.fill = GridBagConstraints.HORIZONTAL;
		
		// Create the file system buttons
		fileSystemGroup.add(selectFiles);
		fileSystemGroup.add(useSQuIREFiles);
		c.ipady = 0;
		c.ipadx = 0;
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		content.add(selectFiles,c);
		c.gridx++;
		content.add(useSQuIREFiles,c);
		selectFiles.setSelected(true);
		
		// Create manual file system panel
		c.gridx = 0;
		c.gridwidth = 2;
		c.gridy++;
		content.add(manualPanel,c);
		
		c.gridy = 0;
		c.gridwidth = 1;
		manualPanel.add(foreField,c);
		
		c.gridy++;
		manualPanel.add(backField,c);
		
		c.gridy++;
		manualPanel.add(imageField,c);
		
		c.gridy++;
		manualPanel.add(saveField,c);
		
		// Create SQuIRE file system panel
		c.gridy = 2;
		c.gridwidth = 2;
		content.add(squirePanel,c);
		
		folderDepth.add(singleChannel);
		folderDepth.add(singleTimePoint);
		folderDepth.add(allTimePoints);
		folderDepth.add(allSamples);
		
		c.gridy = 0;
		c.gridwidth = 1;
		squirePanel.add(singleChannel,c);
		
		c.gridx++;
		squirePanel.add(singleTimePoint,c);
		
		c.gridx++;
		squirePanel.add(allTimePoints,c);
		
		c.gridx++;
		squirePanel.add(allSamples,c);
		
		c.gridwidth = 4;
		c.gridx = 0;
		c.gridy++;
		squirePanel.add(squireFolder,c);
		
		c.gridy++;
		c.gridwidth = 2;
		squirePanel.add(skipProcessedBox,c);
		
		c.gridx = 2;
		squirePanel.add(calculateErrorBox,c);
		
		singleChannel.setSelected(true);
		
		// Add go button
		c.gridy = 3;
		c.gridx = 0;
		c.fill = GridBagConstraints.NONE;
		c.ipadx = 10;
		c.ipady = 10;
		content.add(goButton,c);
		
		squirePanel.setVisible(false);
		manualPanel.setVisible(true);
		
		dialog.add(content,BorderLayout.NORTH);
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
	}
	
	private class goAction implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent paramActionEvent) {
			int level = 0;
			boolean useSquire = useSQuIREFiles.isSelected();
			
			if (useSquire) {
				if (singleChannel.isSelected()) {
					level = 1;
				} else if (singleTimePoint.isSelected()) {
					level = 2;
				} else if (allTimePoints.isSelected()) {
					level = 3;
				} else if (allSamples.isSelected()) {
					level = 4;
				};
				System.out.println("SQuIRE Level: " + level);
				
				SquireFileSystem sFiles = new SquireFileSystem(squireFolder.getValue(),level);

				Thread thread;
				ProgressMonitor pMon = new ProgressMonitor(dialog,"Running CARPE Absorption Calculations...",
														   "",0,sFiles.numFiles());
				pMon.setMillisToDecideToPopup(0);
				pMon.setMillisToPopup(0);
				
				thread = new Thread(new ProcessSquireAbsorption(sFiles,pMon,calculateErrorBox.isSelected()));
				thread.start();
			} else {
				
			}
			
		}
		
	}

	class ProcessSquireAbsorption implements Runnable {
		
		SquireFileSystem sFiles;
		ProgressMonitor progbar;
		boolean saveError;
		
		public ProcessSquireAbsorption(SquireFileSystem files, ProgressMonitor progbar, boolean saveError) {
			this.sFiles = files;
			this.progbar = progbar;
			this.saveError = saveError;
		}
		
		public void run() {
			int progress = 0;
			
			progbar.setProgress(0);
			
			sampLoop:
			while (sFiles.moreSamples()) {
				System.out.println("Sample Dir: " + sFiles.currentSampleDir());
				while (sFiles.moreTimepoints()) {
					System.out.println("Timepoint Dir: " + sFiles.currentTimepointDir());
					while (sFiles.moreChannels()) {
						System.out.println("Channel Dir: " + sFiles.currentChannelDir());
						if (sFiles.invalidChannel()) {
							System.out.println("Channel does not have appropriate image directories. Skipping.");
							sFiles.nextChannel();
							continue;
						}
						while (sFiles.moreImages()) {
							if (progbar.isCanceled()) {
								break sampLoop;
							}
							System.out.println("Current Image: " + sFiles.currentImage());
							progbar.setNote(sFiles.textUpdate());

							if (skipProcessedBox.isSelected() && sFiles.absAlreadyProcessed()) {
								progbar.setProgress(progress++);
								sFiles.nextImage();
								continue;
							}
							ImageStats image = sFiles.getSampleIS();
							ImagePlus absorbance = image.getAbsorbance(sFiles.getForeIS(), sFiles.getBackIS());
							IJ.saveAsTiff(absorbance, sFiles.getAbsorptionDir(true)+File.separator+image.getName());
							
							if (saveError) {
								IJ.saveAsTiff(image.getErrorImage(), sFiles.getAbsorptionDir(true)+File.separator+image.getName()+" Error");
							}
							
							progbar.setProgress(progress++);
							sFiles.nextImage();
						}
						sFiles.nextChannel();
					}
					sFiles.nextTimepoint();
				}
				sFiles.nextSample();
			}
			progbar.setNote("Finished processing images!");
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			progbar.close();

		}
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
	
	private void initElements() {
		// File System Select
		fileSystemGroup = new ButtonGroup();
			useSQuIREFiles = new JToggleButton("Use SQuIRE Images");
			useSQuIREFiles.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent actionEvent) {
					JToggleButton button = (JToggleButton) actionEvent.getSource();
					boolean isVisible = button.getModel().isSelected();
					squirePanel.setVisible(isVisible);
					manualPanel.setVisible(!isVisible);
				}
				
			});
			selectFiles = new JToggleButton("Select Files Manually");
			selectFiles.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent actionEvent) {
					JToggleButton button = (JToggleButton) actionEvent.getSource();
					boolean isVisible = button.getModel().isSelected();
					squirePanel.setVisible(!isVisible);
					manualPanel.setVisible(isVisible);
				}
				
			});
			
		// Panel for SQuIRE file system
		squirePanel = new JPanel(new GridBagLayout());
		squirePanel.setBorder(BorderFactory.createTitledBorder("Use Squire File System"));
			folderDepth = new ButtonGroup();
				singleChannel = new JToggleButton("Single Channel");
				singleChannel.setToolTipText("<html>The folder selected only contains images for a single channel and was captured by SQuIRE.</html>");
				singleTimePoint = new JToggleButton("Single Time Point");
				singleTimePoint.setToolTipText("<html>The folder selected contains all channels collected at a single time point, captured by SQuIRE.</html>");
				allTimePoints = new JToggleButton("All Time Points");
				allTimePoints.setToolTipText("<html>The folder selected contains multiple time points for a particular sample, captured by SQuIRE.</html>");
				allSamples = new JToggleButton("All Samples");
				allSamples.setToolTipText("<html>The folder selected contains multiple samples and time points, captured by SQuIRE.</html>");
			squireFolder = new FileChooserPanel("SQuIRE Folder: ","");
			skipProcessedBox = new JCheckBox("Skip previously processed files ");
			skipProcessedBox.setSelected(true);
			calculateErrorBox = new JCheckBox("Generate error image ");
			calculateErrorBox.setSelected(false);
			
		// Panel for manual selection of files
		manualPanel = new JPanel(new GridBagLayout());
		manualPanel.setBorder(BorderFactory.createTitledBorder("Manually Select Files"));
			foreField = new FileChooserPanel("Foreground Image: ", "");
			backField = new FileChooserPanel("Background Image: ", "");
			imageField = new DirectoryChooserPanel("Image Directory: ","");
			saveField = new DirectoryChooserPanel("Save Directory","");
		
		goButton = new JButton("GO!");
		goButton.setFont(new Font("Arial",Font.BOLD,16));
		goButton.setForeground(new Color(0,201,201));
		goButton.addActionListener(new goAction());
	}

}
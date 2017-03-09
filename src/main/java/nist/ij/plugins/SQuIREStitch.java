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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.ProgressMonitor;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import ij.IJ;
import ij.ImageJ;
import ij.plugin.PlugIn;
import nist.ij.guitools.FileChooserPanel;
import nist.ij.guitools.TextFieldInputPanel;
import nist.ij.guitools.ValidatorInt;
import nist.ij.squire.SquireFileSystem;

public class SQuIREStitch implements PlugIn {

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
	JPanel mistPanel;
	
	// GUI components to use SQuIRE file system
	private FileChooserPanel squireFolder;
	private ButtonGroup folderDepth;
	private JToggleButton singleChannel;
	private JToggleButton singleTimePoint;
	private JToggleButton allTimePoints;
	private JToggleButton allSamples;
	
	// GUI components for MIST Settings
	private TextFieldInputPanel<Integer> vertOverlapInput;
	private TextFieldInputPanel<Integer> horzOverlapInput;
	private JLabel gridOriginBoxLabel;
	private JComboBox gridOriginInput;
	private JCheckBox alignChannelsBox;
		
	// calculate absorption values
	JButton goButton;
	
	// Options for MIST
	String gridwidth;
	String gridheight;
	String filenamepattern;
	String outputMeta = "True";
	String startcol = "0";
	String startrow = "0";
	String programType = "Auto";
	String headless = "True";
	String vertOverlap = "10";
	String horzOverlap = "10";
	String gridOrigin = "LL";
	String[] gridOriginLabels = {"Upper Left", "Upper Right", "Lower Left", "Lower Right"};
	String[] gridOriginValues = {"UL", "UR", "LL", "LR"}; 
	HashMap<String,String> gridOriginHashMap = new HashMap<String,String>();
	ArrayList<String> originKeys = new ArrayList<String>();

	protected static final int flags = 29;
	
	public static void main(String[] args) {
		// set the plugins.dir property to make the plugin appear in the Plugins menu
		Class<?> clazz = SQuIREStitch.class;
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
		System.setProperty("plugins.dir", pluginsDir);

		// start ImageJ
		new ImageJ();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	}
	
	public void showDialog() {
		initElements();
		
		// Set up dialog components
		JFrame dialog = new JFrame();
		dialog.setLayout(new GridBagLayout());
		dialog.setTitle("Quantitative Absorption");
		dialog.setSize(new Dimension(367,281));
		GridBagConstraints c = new GridBagConstraints();
		
		// Basic constraints
		c.insets = new Insets(1, 1, 1, 1);
		c.anchor = GridBagConstraints.NORTH;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		
		// Create SQuIRE file system panel		
		folderDepth.add(singleChannel);
		folderDepth.add(singleTimePoint);
		folderDepth.add(allTimePoints);
		folderDepth.add(allSamples);
		
		c.gridy = 0;
		c.gridx = 0;
		c.gridwidth = 1;
		squirePanel.add(singleChannel,c);
		
		c.gridx++;
		squirePanel.add(singleTimePoint,c);
		
		c.gridx = 0;
		c.gridy++;
		squirePanel.add(allTimePoints,c);
		
		c.gridx++;
		squirePanel.add(allSamples,c);
		
		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy++;
		squirePanel.add(squireFolder,c);
		
		singleChannel.setSelected(true);
		
		c.gridy = 0;
		c.gridwidth = 1;
		c.weightx = 1;
		c.weighty = 1;
		dialog.add(squirePanel,c);
		
		// Create MIST Settings Panel
		c.gridy = 0;
		c.gridx = 0;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.EAST;
		c.fill = GridBagConstraints.NONE;
		mistPanel.add(vertOverlapInput,c);
		
		c.gridx++;
		c.anchor = GridBagConstraints.WEST;
		mistPanel.add(horzOverlapInput,c);
		
		c.gridy++;
		c.gridx = 0;
		c.anchor = GridBagConstraints.EAST;
		mistPanel.add(gridOriginBoxLabel,c);
		
		c.gridx++;
		c.anchor = GridBagConstraints.WEST;
		mistPanel.add(gridOriginInput,c);
		
		c.anchor = GridBagConstraints.NORTH;
		c.fill = GridBagConstraints.BOTH;
		c.gridy = 1;
		c.gridx = 0;
		dialog.add(mistPanel,c);
		
		// Add go button
		c.gridy = 2;
		c.fill = GridBagConstraints.NONE;
		c.ipadx = 10;
		c.ipady = 10;
		dialog.add(goButton,c);
		
		// Display Dialog
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
	}
	
	private class goAction implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent paramActionEvent) {
			int level = 0;
			
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
			
			thread = new Thread(new StitchImages(sFiles));
			thread.start();
			
		}
		
	}

	class StitchImages implements Runnable {
		SquireFileSystem sFiles;
		ProgressMonitor pMon;
		
		public StitchImages(SquireFileSystem sFiles) {
			this.sFiles = sFiles;
			pMon = new ProgressMonitor(dialog,"Stitching images...",
					   				   "",0,sFiles.totalTimepoints());
		}
		
		String well = "";
		int row;
		int col;

		@Override
		public void run() {
			pMon.setMillisToDecideToPopup(0);
			pMon.setMillisToPopup(0);
			int progress = 0;
			pMon.setProgress(progress);
			
			sampLoop:
			while (sFiles.moreSamples()) {
				
				while (sFiles.moreTimepoints()) {
					
					String globalpositionpath = null; 
					
					while (sFiles.moreChannels()) {
						
						if (sFiles.invalidChannel()) {
							
							sFiles.nextChannel();
							continue;
						}
						
						pMon.setProgress(progress++);
						pMon.setNote("Processing timepoint " + Integer.toString(progress) + " of " + Integer.toString(sFiles.totalTimepoints()));

						if (sFiles.getAbsorptionDir(false)==null) {
							System.out.println("Could not find an Absorbance image directory. Skipping folder.");
							sFiles.nextChannel();
							continue;
						
						} else {
							if (globalpositionpath==null) {
								globalpositionpath = sFiles.getStitchDir(true); 
							}
						}
						
						ArrayList<String> wells = new ArrayList<String>();
						ArrayList<Integer> maxCol = new ArrayList<Integer>();
						ArrayList<Integer> maxRow = new ArrayList<Integer>();
						int wellIndex = 0;
						
						// Loop through images to find the gridwidth and height
						while (sFiles.moreImages()) {
							well = sFiles.currentImageWell();
							
							if (well==null) {
								sFiles.nextImage();
								continue;
							}
							
							row = sFiles.currentImageRow();
							col = sFiles.currentImageCol();
							
							if (wells.contains(well)) {
								wellIndex = wells.indexOf(well);
							} else {
								wells.add(sFiles.currentImageWell());
								maxCol.add(col);
								maxRow.add(row);
								wellIndex = wells.size()-1;
								sFiles.nextImage();
								continue;
							}
							
							if (row>maxRow.get(wellIndex)) {
								maxRow.set(wellIndex,row);
							}
							
							if (col>maxCol.get(wellIndex)) {
								maxCol.set(wellIndex,col);
							}
							
							sFiles.nextImage();
						}
						
						if (wells.size()==0) {
							sFiles.nextImage();
							continue;
						}
						
						for (int well = 0; well<wells.size(); well++) {
							System.out.println("Well: " + wells.get(well));
							System.out.println("Max Rows: " + maxRow.get(well));
							System.out.println("Max Cols: " + maxCol.get(well));
							
							String options = "";
							
							System.out.println("Stitching Options:");
							
							String fileNamePattern = "filenamepattern="+wells.get(well)+"_r{rrr}_c{ccc}.tif";
							options += fileNamePattern;
							
							String filePatternType = " filepatterntype=ROWCOL";
							options += filePatternType;
							
							if (sFiles.currentChannelIndex()>0) {
								String assembleMetaData = " assemblefrommetadata=true";
								options += assembleMetaData;
								
								String metaFile = " globalpositionsfile=[" + globalpositionpath + File.separator + wells.get(well) + "-global-positions-1.txt]";
								options += metaFile;
							}
							
							String programType = " programtype=AUTO";
							options += programType;
							
							String imageDir = " imagedir=[" + sFiles.getAbsorptionDir(false) + "]";
							options += imageDir;
							
							String gridWidth = " gridwidth=" + Integer.toString(maxCol.get(well)+1);
							options += gridWidth;
							
							String gridHeight = " gridheight=" + Integer.toString(maxRow.get(well)+1);
							options += gridHeight;
							
							String outputPath = " outputpath=[" + sFiles.getStitchDir(true) + "]";
							options += outputPath;
							
							String outputMeta = " outputmeta=true";
							options += outputMeta;
							
							String startRow = " startrow=0";
							options += startRow;
							
							String startCol = " startcol=0";
							options += startCol;
							
							String extentWidth = " extentwidth=" + Integer.toString(maxCol.get(well)+1);
							options += extentWidth;
							
							String extentHeight = " extentheight=" + Integer.toString(maxRow.get(well)+1);
							options += extentHeight;
							
							String suppressSub = " issuppresssubgridwarningenabled=true";
							options += suppressSub;
							
							String startTile = " starttile=0";
							options += startTile;
							
							String isHeadless = " headless=true";
							options += isHeadless;
							
							String gridOrigin = " gridorigin=" + gridOriginHashMap.get(gridOriginInput.getSelectedItem());
							options += gridOrigin;
							
							String horizontaloverlap = " horiztonaloverlap=" + horzOverlapInput.getValue().toString();
							options += horizontaloverlap;
							
							String verticaloverlap = " verticaloverlap=" + vertOverlapInput.getValue().toString();
							options += verticaloverlap;
							
							String outputfullimage = " outputfullimage=true";
							options += outputfullimage;
							
							String outfileimage = " outfileprefix=" + wells.get(well) + "-";
							options += outfileimage;
							
							String dispLog = " loglevel=none";
							options += dispLog;
							
							if (pMon.isCanceled()) {
								break sampLoop;
							}
							
							IJ.run("MIST",options);
						}
						sFiles.nextChannel();
					}
					sFiles.nextTimepoint();
				}
				sFiles.nextSample();
			}
			
			pMon.close();
			
			// Find the grid width and height according to 
			
/*			String options = "imagedir=" + sFiles.currentChannel() +
						     " gridwidth=" + ;*/
			
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
	
	private void initElements() {
		
		for (int i = 0; i < gridOriginLabels.length; i++) {
			gridOriginHashMap.put(gridOriginLabels[i], gridOriginValues[i]);
		}

		// Panel for SQuIRE file system
		squirePanel = new JPanel(new GridBagLayout());
		squirePanel.setBorder(BorderFactory.createTitledBorder("Squire File System"));
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
			
		mistPanel = new JPanel(new GridBagLayout());
		mistPanel.setBorder(BorderFactory.createTitledBorder("MIST Settings"));
			vertOverlapInput = new TextFieldInputPanel<Integer>("Vertical Overlap: ", vertOverlap, 3, new ValidatorInt(1,50));
			vertOverlapInput.setToolTipText("<html>Indicate the amount of vertical overlap as a percentage of the image.</html>");
			horzOverlapInput = new TextFieldInputPanel<Integer>("Horizontal Overlap: ", horzOverlap, 3, new ValidatorInt(1,50));
			horzOverlapInput.setToolTipText("<html>Indicate the amount of horizontal overlap as a percentage of the image.</html>");
			gridOriginBoxLabel = new JLabel("Grid Origin: ");
			gridOriginInput = new JComboBox(gridOriginLabels);
			gridOriginInput.setSelectedIndex(1);
			
		goButton = new JButton("GO!");
		goButton.setFont(new Font("Arial",Font.BOLD,16));
		goButton.setForeground(new Color(0,201,201));
		goButton.addActionListener(new goAction());
	}
	
}

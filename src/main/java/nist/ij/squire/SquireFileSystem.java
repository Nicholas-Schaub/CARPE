package nist.ij.squire;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ij.IJ;

public class SquireFileSystem {
	// Constants
	public static final int SINGLECHANNEL = 1;
	public static final int SINGLETIMEPOINT = 2;
	public static final int ALLTIMEPOINTS = 3;
	public static final int ALLSAMPLES = 4;
	public static final String CALIBRATION_DIR = "Calibration Images";
	public static final String SEGMENTATION_DIR = "Segmented Images";
	public static final String RAW_DIR = "Raw Images";
	public static final String ABSORPTION_DIR = "Absorption Images";
	public static final String STITCH_DIR = "Stitch Images";
	public static final String FILESEP = Pattern.quote(File.separator);
	
	public static final String FORE_RAW_FILE = "Light Background.tif";
	public static final String FORE_RAW_FILE_OLD = "LightBlank.tif";
	public static final String FORE_MEAN_FILE = "Light Background-Mean.tif";
	public static final String FORE_STD_FILE = "Light Background-STD.tif";
	public static final String BACK_FILE = "Dark Background.tif";
	public static final String BACK_FILE_OLD = "DarkBlank.tif";
	public static final String LINREG_FILE = "Linear Regression.tif";
	public static final String LINREG_FILE_OLD = "LinReg.tif";
	
	// Directory and subdirectories
	private ArrayList<ArrayList<ArrayList<File[]>>> imageFiles;
	private ArrayList<ArrayList<ArrayList<File[]>>> calibrationFiles;
	private ArrayList<ArrayList<File[]>> channels;
	private ArrayList<File[]> timepoints;
	private File[] samples;
	
	// File index
	private int numFiles = 0;
	private int dirIndex = 0;
	private int sampIndex = 0;
	private int timeIndex = 0;
	private int chanIndex = 0;
	private Pattern p = Pattern.compile("(\\w+)_r(\\d+)_c(\\d+).tif+");
	private boolean newFore = true;
	private boolean newBack = true;
	private int imageIndex = 0;
	private boolean newImage = true;
	private ImageStats currentImage;
	private ImageStats currentFore;
	private ImageStats currentBack;
	private String fileType = "tif";
	
	public SquireFileSystem(String filePath, int type) {
		if (type>ALLSAMPLES || type<SINGLECHANNEL) {throw new IndexOutOfBoundsException();}
		
		imageFiles = new ArrayList<ArrayList<ArrayList<File[]>>>();
		calibrationFiles = new ArrayList<ArrayList<ArrayList<File[]>>>();
		timepoints = new ArrayList<File[]>();
		channels = new ArrayList<ArrayList<File[]>>();
				
		if (type==ALLSAMPLES) {
			samples = new File(filePath).listFiles(new FileFilter(){

				@Override
				public boolean accept(File file) {
					return file.isDirectory();
				}
				
			});
		} else {
			samples = new File[1];
			samples[0] = new File(filePath);
		}
		
		if (type>=ALLTIMEPOINTS) {
			for (File sample : samples) {
				File[] timePaths = new File(sample.getAbsolutePath()).listFiles(new FileFilter(){

					@Override
					public boolean accept(File file) {
						return file.isDirectory();
					}
					
				});
				
				timepoints.add(timePaths);
			}
		} else {
			timepoints.add(new File[1]);
			timepoints.get(0)[0] = new File(filePath);
		}
		
		if (type>=SINGLETIMEPOINT) {
			for (File[] sampleTimepoints : timepoints) {
				channels.add(new ArrayList<File[]>());
				for (File timepoint : sampleTimepoints) {
					File[] chanPaths = new File(timepoint.getAbsolutePath()).listFiles(new FileFilter(){

						@Override
						public boolean accept(File file) {
							return file.isDirectory();
						}
						
					});
					
					channels.get(channels.size()-1).add(chanPaths);
				}
			}
		} else {
			channels.add(new ArrayList<File[]>());
			channels.get(0).add(new File[1]);
			channels.get(0).get(0)[0] = new File(filePath);
		}
				
		for (ArrayList<File[]> sample : channels) {
			imageFiles.add(new ArrayList<ArrayList<File[]>>());
			calibrationFiles.add(new ArrayList<ArrayList<File[]>>());
			for (File[] timepoint : sample) {
				imageFiles.get(imageFiles.size()-1).add(new ArrayList<File[]>());
				calibrationFiles.get(calibrationFiles.size()-1).add(new ArrayList<File[]>());
				for (File channel : timepoint) {
					File[] chanSubs = new File(channel.getAbsolutePath()).listFiles(new FileFilter(){

						@Override
						public boolean accept(File file) {
							String name = file.getName();
							
							if (name.contentEquals(CALIBRATION_DIR) || name.contentEquals(RAW_DIR)) {
								return true;
							} else {
								return false;
							}
						}
						
					});
					
					if (chanSubs==null) {
						continue;
					}
					
					int isValid = 0;
					for (File folder : chanSubs) {
						if (folder.getName().toLowerCase().contentEquals(CALIBRATION_DIR.toLowerCase()) ||
							folder.getName().toLowerCase().contentEquals(RAW_DIR.toLowerCase())) {
							isValid++;
						}
					}
					
					if (isValid!=2) {
						imageFiles.get(imageFiles.size()-1).get(imageFiles.get(imageFiles.size()-1).size()-1).add(null);
						calibrationFiles.get(calibrationFiles.size()-1).get(calibrationFiles.get(calibrationFiles.size()-1).size()-1).add(null);
						System.out.println("Invalid Directory: " + channel.getAbsolutePath());
						continue;
					}
					
					File[] sampFiles = new File(channel.getAbsolutePath()+File.separator+RAW_DIR).listFiles(new FileFilter(){

						@Override
						public boolean accept(File file) {
							String name = file.getName();
							if (name.toLowerCase().endsWith(fileType.toLowerCase())) {
								numFiles++;
								return true;
							} else {
								return false;
							}
						}
						
					});
					
					File[] calFiles = new File(channel.getAbsolutePath()+File.separator+CALIBRATION_DIR).listFiles(new FileFilter(){
						@Override
						public boolean accept(File file) {
							String name = file.getName();
							if (name.toLowerCase().endsWith(fileType.toLowerCase())) {
								return true;
							} else {
								return false;
							}
						}
					});
					imageFiles.get(imageFiles.size()-1).get(imageFiles.get(imageFiles.size()-1).size()-1).add(sampFiles);
					calibrationFiles.get(calibrationFiles.size()-1).get(calibrationFiles.get(calibrationFiles.size()-1).size()-1).add(calFiles);
				}
			}
		}
	}
	
	public String currentSampleDir() {
		return samples[sampIndex].getAbsolutePath();
	}
	
	public String currentTimepointDir() {
		return timepoints.get(sampIndex)[timeIndex].getAbsolutePath();
	}
	
	public String currentChannelDir() {
		return channels.get(sampIndex).get(timeIndex)[chanIndex].getAbsolutePath();
	}
	
	public String currentImageDir() {
		return imageFiles.get(sampIndex).get(timeIndex).get(chanIndex)[imageIndex].getAbsolutePath();
	}
	
	public String currentSample() {
		return samples[sampIndex].getName();
	}
	
	public String currentTimepoint() {
		return timepoints.get(sampIndex)[timeIndex].getName();
	}
	
	public String currentChannel() {
		return channels.get(sampIndex).get(timeIndex)[chanIndex].getName();
	}
	
	public String currentImage() {
		return imageFiles.get(sampIndex).get(timeIndex).get(chanIndex)[imageIndex].getName();
	}
	
	public String currentImageWell() {
		Matcher m = p.matcher(currentImage());
		if (m.matches()) {
			return m.group(1);
		} else {
			return null;
		}
	}
	
	public int currentImageRow() {
		Matcher m = p.matcher(currentImage());
		if (m.matches()) {
			return Integer.parseInt(m.group(2));
		} else {
			return 0;
		}
	}
	
	public int currentImageCol() {
		Matcher m = p.matcher(currentImage());
		if (m.matches()) {
			return Integer.parseInt(m.group(3));
		} else {
			return 0;
		}
	}
	
	public int numSamples() {
		return samples.length;
	}
	
	public int numTimepoints() {
		return timepoints.get(sampIndex).length;
	}
	
	public int numChannels() {
		return channels.get(sampIndex).get(timeIndex).length;
	}
	
	public int numImages() {
		return imageFiles.get(sampIndex).get(timeIndex).get(chanIndex).length;
	}
	
	public void prevSample() {
		sampIndex--;
		resetTimepointIndex();
		if (sampIndex<0) {
			sampIndex++;
		}
	}
	
	public void nextSample() {
		sampIndex++;
		resetTimepointIndex();
		if (sampIndex>numSamples()) {
			sampIndex--;
		}
	}
	
	public void prevTimepoint() {
		timeIndex--;
		resetChannelIndex();
		if (timeIndex<0) {
			timeIndex++;
		}
	}
	
	public void nextTimepoint() {
		timeIndex++;
		resetChannelIndex();
		if (timeIndex>numTimepoints()) {
			timeIndex--;
		}
	}
	
	public void prevChannel() {
		chanIndex--;
		resetImageIndex();
		if (chanIndex<0) {
			chanIndex++;
		} else {
			newFore = true;
		}
	}
	
	public void nextChannel() {
		chanIndex++;
		resetImageIndex();
		if (chanIndex>numChannels()) {
			chanIndex--;
		} else {
			newFore = true;
		}
	}
	
	public void nextImage() {
		imageIndex++;
		newImage = true;
		if (imageIndex>numImages()) {
			imageIndex--;
		}
	}
	
	public void nextDir() {
		if (moreChannels()) {
			nextChannel();
			resetImageIndex();
			return;
		}
		
		if (moreTimepoints()) {
			nextTimepoint();
			resetChannelIndex();
			resetImageIndex();
			return;
		}
		
		if (moreSamples()) {
			nextSample();
			resetTimepointIndex();
			resetChannelIndex();
			resetImageIndex();
			return;
		}
	}
	
	public int currentSampleIndex() {
		return sampIndex;
	}
	
	public int currentTimepointIndex() {
		return timeIndex;
	}
	
	public int currentChannelIndex() {
		return chanIndex;
	}
	
	public int currentImageIndex() {
		return imageIndex;
	}
	
	public void resetSampIndex() {
		resetTimepointIndex();
		sampIndex = 0;
	}
	
	public void resetTimepointIndex() {
		resetChannelIndex();
		timeIndex = 0;
	}
	
	public void resetChannelIndex() {
		resetImageIndex();
		newFore = true;
		currentFore = null;
		newBack = true;
		currentBack = null;
		chanIndex = 0;
	}
	
	public void resetImageIndex() {
		newImage = true;
		currentImage = null;
		imageIndex = 0;
	}
	
	public boolean moreSamples() {
		if (sampIndex==numSamples()) {
			return false;
		} else {
			return true;
		}
	}
	
	public boolean moreTimepoints() {
		if (timeIndex==numTimepoints()) {
			return false;
		} else {
			return true;
		}
	}
	
	public boolean moreChannels() {
		if (chanIndex==numChannels()) {
			return false;
		} else {
			return true;
		}
	}
	
	public boolean invalidChannel() {
		if (imageFiles.get(sampIndex).get(timeIndex).get(chanIndex)==null) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean moreImages() {
		if (imageIndex==numImages()) {
			return false;
		} else {
			return true;
		}
	}
	
	public ImageStats getSampleIS() {
		if (newImage) {
			newImage = false;
			return new ImageStats(IJ.openImage(imageFiles.get(sampIndex).get(timeIndex).get(chanIndex)[imageIndex].getAbsolutePath()));
		} else {
			return currentImage;
		}
		
	}
	
	public ImageStats getForeIS() {
		if (!newFore) {
			return currentFore;
		}
		for (int file = 0; file<calibrationFiles.get(sampIndex).get(timeIndex).get(chanIndex).length; file++) {
			File temp = calibrationFiles.get(sampIndex).get(timeIndex).get(chanIndex)[file];
			if (temp.getName().endsWith(FORE_MEAN_FILE)) {
				System.out.println("Found mean image!");
				for (int bfile = 0; bfile<calibrationFiles.get(sampIndex).get(timeIndex).get(chanIndex).length; bfile++) {
					File btemp = calibrationFiles.get(sampIndex).get(timeIndex).get(chanIndex)[bfile];
					System.out.println(btemp.getName());
					if (btemp.getName().endsWith(FORE_STD_FILE)) {
						System.out.println("Found std image!");
						currentFore = new ImageStats(IJ.openImage(temp.getAbsolutePath()),IJ.openImage(btemp.getAbsolutePath()));
						newFore = false;
						return currentFore;
					}
				}
			} else if (temp.getName().endsWith(FORE_RAW_FILE) || temp.getName().endsWith(FORE_RAW_FILE_OLD)) {
				currentFore = new ImageStats(IJ.openImage(temp.getAbsolutePath()));
				newFore = false;
				return currentFore;
			}
		}
		return null;
	}
	
	public ImageStats getBackIS() {
		if (!newBack) {
			return currentBack;
		}
		for (int file = 0; file<calibrationFiles.get(sampIndex).get(timeIndex).get(chanIndex).length; file++) {
			File temp = calibrationFiles.get(sampIndex).get(timeIndex).get(chanIndex)[file];
			if (temp.getName().endsWith(BACK_FILE) || temp.getName().endsWith(BACK_FILE_OLD)) {
				newBack = false;
				currentBack = new ImageStats(IJ.openImage(temp.getAbsolutePath()));
				return currentBack;
			}
		}
		return null;
	}
	
	public String getStitchDir(boolean createDirectory) {
		File f = new File(currentChannelDir()+File.separator+STITCH_DIR);
		if (!f.isDirectory() && createDirectory) {
			f.mkdir();
		} else if (!f.isDirectory()) {
			return null;
		}
		
		return f.getAbsolutePath();
	}
	
	public String getAbsorptionDir(boolean createDirectory) {
		File f = new File(currentChannelDir()+File.separator+ABSORPTION_DIR);
		if (!f.isDirectory() && createDirectory) {
			f.mkdir();
		} else if (!f.isDirectory()) {
			return null;
		}
		
		return f.getAbsolutePath();
	}
	
	public String getSegmentDir(boolean createDirectory) {
		File f = new File(currentChannelDir()+File.separator+SEGMENTATION_DIR);
		if (!f.isDirectory() && createDirectory) {
			f.mkdir();
		} else if (!f.isDirectory()) {
			return null;
		}
		
		return f.getAbsolutePath();
	}
	
	public boolean absAlreadyProcessed() {
		
		if (getAbsorptionDir(false)==null) {
			return false;
		}
		
		File[] f = new File(getAbsorptionDir(false)).listFiles(new FileFilter(){

			@Override
			public boolean accept(File file) {
				String name = file.getName();
				if (name.toLowerCase().matches(imageFiles.get(sampIndex).get(timeIndex).get(chanIndex)[imageIndex].getName().toLowerCase())) {
					System.out.println("Found processed file: " + imageFiles.get(sampIndex).get(timeIndex).get(chanIndex)[imageIndex].getName().toLowerCase());
					return true;
				} else {
					return false;
				}
			}
			
		});
		if (f.length>0) {
			return true; 
		} else {
			return false;
		}
	}
	
	public int numFiles() {
		return numFiles;
	}
	
	public String textUpdate() {
		String t = null;
		if (samples.length>1) {
			t = "<html>Processing multiple samples.<br>";
			t += "Current Sample: " + samples[sampIndex].getName() + "<br>";
		}
		
		if (timepoints.get(sampIndex).length>1 && t==null) {
			t = "<html>Processing multiple timepoints.<br>";
		}
		
		if (timepoints.get(sampIndex).length>1) {
			t += "Current Timepoint: " + timepoints.get(sampIndex)[timeIndex].getName() + "<br>";
		}
		
		if (channels.get(sampIndex).get(timeIndex).length>1 && t==null) {
			t = "<html>Processing single timepoint.<br>";
		}
		
		if (channels.get(sampIndex).get(timeIndex).length>1) {
			t += "Current Channel: " + channels.get(sampIndex).get(timeIndex)[chanIndex].getName() + "<br>";
		}
		
		if (imageFiles.get(sampIndex).get(timeIndex).get(chanIndex).length>1 && t==null) {
			t = "<html>Processing single channel.<br>";
		}
		
		t += "Current Image: " + imageFiles.get(sampIndex).get(timeIndex).get(chanIndex)[imageIndex].getName() + "<br>";
		t += "</html>";
		
		return t;
	}

}

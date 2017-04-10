CARPE is an ImageJ plugin designed to calculate absorbance values from images collected on a bright field microscope. CARPE stands for Calculation of Absorbance of Retinal Pigment Epithelium.

Eclipe Setup
============
0. Before you begin, install dependencies if they are not already installed
	A. Install FIJI: https://fiji.sc/
	B. Once FIJI is installed, install MIST plugin by selecting '''Help''' > '''Update''' > '''Manage update sites''' > Find MIST, select the check box, close window and select apply changes
	C. Install JDK 1.8.
1. Clone the git repository
	A. Switch to the Git perspective in Eclipse.
	B. In the Git Repositories tab, click the button to clone the git repository.
	C. In the URI: '''https://github.com/Nicholas-Schaub/CARPE.git'''
2. Import the project
	A. In the Git perspective, right click the CARPE git and select '''Import Project...'''.
	B. Import as a general project. Click Next, then Finish.
	C. Switch to the Java perspective.
	D. Right click the CARPE project, select '''Configure''' > '''Convert to Maven Project'''
	E. Right click the CARPE project, select '''Maven''' > '''Update Project'''
	F. Right click the CARPE project, select '''Refresh'''
3. Setup the Run configuration - a run configuration can be set up for each function in the CARPE plugin
	A. Right click the CARPE project, select '''Run As''' > '''Run Configurations...'''
	B. In the '''Arguments''' tab under '''Working directory''', select '''Other:''' and paste the Fiji directory location into the text box
	C. In the '''Common''' tab under '''Display in favorites menu''', select the '''Run''' checkbox.
	D. Switch to the '''Main''' tab.
		i. To run the '''AbsorptionImages''' plugin, change the '''Main class''' to '''nist.ij.plugins.AbsorptionImage'''
		ii. To run the '''SQuIREStitch''' plugin, change the '''Main class''' to '''nist.ij.plugins.SQuIREStitch'''
	E. Click '''Apply'''
	F. To run the plugin, select the down arrow next to the green run button. Then select whatever you named the run configuration as.
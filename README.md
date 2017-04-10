CARPE is an ImageJ plugin designed to calculate absorbance values from images collected on a bright field microscope. __CARPE__ stands for __C__alculation of __A__bsorbance of __R__etinal __P__igment __E__pithelium.

Eclipe Setup
============
__Before you begin, install dependencies if they are not already installed__
1. Install FIJI: https://fiji.sc/
2. Once FIJI is installed, install MIST plugin by selecting: _Help_ > _Update_ > _Manage update sites_ > Find MIST, select the check box, close window and select apply changes
3. Install JDK 1.8.

__Clone the git repository__
 1. Switch to the Git perspective in Eclipse.
 2. In the Git Repositories tab, click the button to clone the git repository.
 3. In the URI: ``https://github.com/Nicholas-Schaub/CARPE.git``
  
__Import the project__
1. In the Git perspective, right click the CARPE git and select _Import Project..._.
2. Import as a general project. Click Next, then Finish.
3. Switch to the Java perspective.
4. Right click the CARPE project, select _Configure_ > _Convert to Maven Project_
5. Right click the CARPE project, select _Maven_ > _Update Project_
6. Right click the CARPE project, select _Refresh_

__Setup the Run configuration__
1. Right click the CARPE project, select _Run As_ > _Run Configurations..._
2. In the _Arguments_ tab under _Working directory_, select _Other_ and paste the Fiji directory location into the text box
3. In the _Common_ tab under _Display in favorites menu_, select the _Run_ checkbox.
4. Switch to the _Main_ tab.
5. To run the _AbsorptionImages_ plugin, change the _Main class_ to ``nist.ij.plugins.AbsorptionImage``
6. To run the _SQuIREStitch_ plugin, change the _Main class_ to ``nist.ij.plugins.SQuIREStitch``
7. Click _Apply_
8. To run the plugin, select the down arrow next to the green run button. Then select whatever you named the run configuration as.
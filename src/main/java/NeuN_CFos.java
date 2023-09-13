import NeuN_CFos_Tools.Cell;
import NeuN_CFos_Tools.Tools;
import ij.*;
import ij.plugin.PlugIn;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;
import loci.plugins.BF;
import loci.plugins.in.ImporterOptions;
import loci.plugins.util.ImageProcessorReader;
import mcib3d.geom2.Objects3DIntPopulation;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;


/**
* Detect NeuN and CFos cells and compute their colocalization
* Give volume and intensity of each NeuN+, CFos+ or NeuN+/CFos+ cell
* @author ORION-CIRB
*/
public class NeuN_CFos implements PlugIn {

    private NeuN_CFos_Tools.Tools tools = new Tools();
       
    public void run(String arg) {
        try {
            if (!tools.checkInstalledModules() || !tools.checkStardistModels(tools.stardistModel)) {
                return;
            }
            
            String imageDir = IJ.getDirectory("Choose images directory")+File.separator;
            if (imageDir == null) {
                return;
            }
            
            // Find images with fileExt extension
            String fileExt = tools.findImageType(imageDir);
            ArrayList<String> imageFiles = tools.findImages(imageDir, fileExt);
            if (imageFiles.isEmpty()) {
                IJ.showMessage("Error", "No images found with " + fileExt + " extension");
                return;
            }
            
            // Create OME-XML metadata store of the latest schema version
            ServiceFactory factory;
            factory = new ServiceFactory();
            OMEXMLService service = factory.getInstance(OMEXMLService.class);
            IMetadata meta = service.createOMEXMLMetadata();
            ImageProcessorReader reader = new ImageProcessorReader();
            reader.setMetadataStore(meta);
            reader.setId(imageFiles.get(0));
            
            // Find image calibration
            tools.findImageCalib(meta);
            
            // Find channel names
            String[] channelNames = tools.findChannels(imageFiles.get(0), meta, reader);
            
            // Generate dialog box
            String[] channels = tools.dialog(imageDir, channelNames);
            if (channels == null) {
                IJ.showStatus("Plugin canceled");
                return;
            }
            
            // Create output folder
            String outDirResults = imageDir + File.separator + "Results_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) + File.separator;
            File outDir = new File(outDirResults);
            if (!Files.exists(Paths.get(outDirResults))) {
                outDir.mkdir();
            }
            
            // Write headers results for results files
            FileWriter fwResults = new FileWriter(outDirResults + "results.csv", false);
            BufferedWriter results = new BufferedWriter(fwResults);
            results.write("Image name\tImage vol (µm3)\tNeuN bg\tCFos bg\tCell label\tIs Neun?\tNeuN vol (µm3)\tNeuN integrated int\t" +
                          "NeuN bg corrected integrated int\tIs CFos?\tCFos vol (µm3)\tCFos integrated int\tCFos bg corrected integrated int\n");
            results.flush();
            
            for (String f: imageFiles) {
                String rootName = FilenameUtils.getBaseName(f);
                tools.print("--- ANALYZING IMAGE " + rootName + " ------");
                reader.setId(f);
                
                ImporterOptions options = new ImporterOptions();
                options.setId(f);
                options.setSplitChannels(true);
                options.setQuiet(true);
                options.setColorMode(ImporterOptions.COLOR_MODE_GRAYSCALE);
                
                // Analyze NeuN channel
                tools.print("- Analyzing NeuN channel -");
                int indexCh = ArrayUtils.indexOf(channelNames, channels[0]);
                ImagePlus imgNeun = BF.openImagePlus(options)[indexCh];
                double neunBg = tools.findBackground(imgNeun);
                Objects3DIntPopulation neunPop = tools.stardistDetection(imgNeun, tools.minNeunVol, Double.MAX_VALUE);
                
                // Analyze CFos channel
                tools.print("- Analyzing CFos channel -");
                indexCh = ArrayUtils.indexOf(channelNames, channels[1]);
                ImagePlus imgCfos = BF.openImagePlus(options)[indexCh];
                double cfosBg = tools.findBackground(imgCfos);
                Objects3DIntPopulation cfosPop = tools.stardistDetection(imgCfos, tools.minCfosVol, Double.MAX_VALUE);
                
                // Colocalize NeuN and CFos cells
                tools.print("- Colocalizing NeuN and CFos cells -");
                ArrayList<Cell> cells = tools.colocalization(neunPop, cfosPop);
                tools.computeParams(cells, imgNeun, imgCfos, neunBg, cfosBg);
                
                // Write results
                tools.print("- Writing and drawing results -");
                double imgVol = imgNeun.getWidth() * imgNeun.getHeight() * imgNeun.getNSlices() * tools.pixVol;
                for(Cell cell: cells) {
                    HashMap<String, Double> params = cell.getParams();
                    results.write(rootName+"\t"+imgVol+"\t"+neunBg+"\t"+cfosBg+"\t"+params.get("label").intValue()+"\t"+
                            cell.isNeun()+"\t"+params.get("neunVol")+"\t"+params.get("neunInt")+"\t"+params.get("neunCorrInt")+"\t"+
                            cell.isCfos()+"\t"+params.get("cfosVol")+"\t"+params.get("cfosInt")+"\t"+params.get("cfosCorrInt")+"\n");                                
                    results.flush();
                }
                
                // Draw results
                tools.drawResults(cells, imgNeun, imgCfos, outDirResults+rootName+".tif");
                
                tools.closeImage(imgNeun);
                tools.closeImage(imgCfos);
            }
            results.close();
        } catch (IOException | DependencyException | ServiceException | FormatException ex) {
            Logger.getLogger(NeuN_CFos.class.getName()).log(Level.SEVERE, null, ex);
        }
        tools.print("All done!");
    }
}

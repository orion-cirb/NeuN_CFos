package NeuN_CFos_Tools;

import java.util.HashMap;
import mcib3d.geom2.Object3DInt;
import mcib3d.geom2.measurements.MeasureIntensity;
import mcib3d.geom2.measurements.MeasureVolume;
import mcib3d.image3d.ImageHandler;


/**
 * @author ORION-CIRB
 */
public class Cell {
    
    private boolean isNeun;
    private boolean isCfos;
    private Object3DInt neun;
    private Object3DInt cfos;
    private HashMap<String, Double> params;
    

    public Cell() {
        this.isNeun = false;
        this.isCfos = false;
        this.neun = null;
        this.cfos = null;
        this.params = new HashMap<>();
    }
    
    public Object3DInt getNeun() {
        return neun;
    }
    
    public Object3DInt getCfos() {
        return cfos;
    }
    
    public HashMap<String, Double> getParams() {
        return params;
    }
    
    public boolean isNeun() {
        return isNeun;
    }
    
    public boolean isCfos() {
        return isCfos;
    }
    
    public void setNeun(Object3DInt neun) {
        this.isNeun = true;
        this.neun = neun;
    }
    
    public void setCfos(Object3DInt cfos) {
        this.isCfos = true;
        this.cfos = cfos;
    }
    
    public void setParams(double label, ImageHandler imhNeun, ImageHandler imhCfos, double neunBg, double cfosBg) {
        params.put("label", label);
        
        double neunVol = (isNeun)? new MeasureVolume(this.neun).getVolumeUnit() : Double.NaN;
        double neunInt = (isNeun)? new MeasureIntensity(this.neun, imhNeun).getValueMeasurement(MeasureIntensity.INTENSITY_SUM) : Double.NaN;
        double neunCorrInt = (isNeun)? neunInt - neunBg * new MeasureVolume(this.neun).getVolumePix() : Double.NaN;  
        params.put("neunVol", neunVol);
        params.put("neunInt", neunInt);  
        params.put("neunCorrInt", neunCorrInt);  
        
        double cfosVol = (isCfos)? new MeasureVolume(this.cfos).getVolumeUnit() : Double.NaN;
        double cfosInt = (isCfos)? new MeasureIntensity(this.cfos, imhCfos).getValueMeasurement(MeasureIntensity.INTENSITY_SUM) : Double.NaN;
        double cfosCorrInt = (isCfos)? cfosInt - cfosBg * new MeasureVolume(this.cfos).getVolumePix() : Double.NaN;  
        params.put("cfosVol", cfosVol);
        params.put("cfosInt", cfosInt);  
        params.put("cfosCorrInt", cfosCorrInt);  
    }
    
}
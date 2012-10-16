/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 

package org.dawb.passerelle.actors.dawn;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.dawb.common.ui.plot.AbstractPlottingSystem;
import org.dawb.common.ui.plot.PlottingFactory;
import org.dawb.common.ui.plot.region.IRegion;
import org.dawb.common.ui.plot.region.IRegion.RegionType;
import org.dawb.common.ui.plot.tool.IToolPage.ToolPageRole;
import org.dawb.passerelle.common.actors.AbstractDataMessageTransformer;
import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.passerelle.common.message.MessageUtils;
import org.eclipse.swt.widgets.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import uk.ac.diamond.scisoft.analysis.SDAPlotter;
import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.roi.EllipticalFitROI;
import uk.ac.diamond.scisoft.analysis.roi.EllipticalROI;
import uk.ac.diamond.scisoft.analysis.roi.LinearROI;
import uk.ac.diamond.scisoft.analysis.roi.PointROI;
import uk.ac.diamond.scisoft.analysis.roi.ROIBase;
import uk.ac.diamond.scisoft.analysis.roi.RectangularROI;
import uk.ac.diamond.scisoft.analysis.roi.SectorROI;

import com.isencia.passerelle.actor.ProcessingException;

public class PlotImageActor	extends AbstractDataMessageTransformer{

	private static final long serialVersionUID = 4457133165062873343L;
	private StringParameter plotViewName;
	private Parameter hasROIParam;
	protected static final List<String> HAS_ROI;
	static {
		HAS_ROI = new ArrayList<String>(2);
		HAS_ROI.add("Plot the image with ROI(s)");
		HAS_ROI.add("Plot the image without ROI(s)");
	}
	private Parameter boxROITypeParam;
	protected static final List<String> BOX_ROI_TYPE;
	static {
		BOX_ROI_TYPE = new ArrayList<String>(3);
		BOX_ROI_TYPE.add("Box Region Of Interest");
		BOX_ROI_TYPE.add("X-Axis Selection Region Of Interest");
		BOX_ROI_TYPE.add("Y-Axis Selection Region Of Interest");
	}

	private Parameter dataNameParam;
	private Parameter xaxisNameParam;
	private Parameter yaxisNameParam;

	Logger logger = LoggerFactory.getLogger(PlotImageActor.class);


	public PlotImageActor(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);
	
		// Plot View name parameter
		plotViewName = new StringParameter(this, "Name");
		plotViewName.setExpression("Plot 1");
		plotViewName.setDisplayName("Plot View Name");
		registerConfigurableParameter(plotViewName);

		// data name parameter
		dataNameParam = new StringParameter(this, "Data Name");
		dataNameParam.setDisplayName("Data Name");
		registerConfigurableParameter(plotViewName);

		// xaxis/yaxis data name parameter
		xaxisNameParam = new StringParameter(this, "X-Axis Data Name");
		xaxisNameParam.setDisplayName("X-Axis Data Name");
		registerConfigurableParameter(xaxisNameParam);
		yaxisNameParam = new StringParameter(this, "Y-Axis Data Name");
		yaxisNameParam.setDisplayName("Y-Axis Data Name");
		registerConfigurableParameter(yaxisNameParam);

		// ROI option parameter
		hasROIParam = new StringParameter(this,"Region Of Interest") {
			private static final long serialVersionUID = 2815254879307619914L;
			public String[] getChoices() {
				return HAS_ROI.toArray(new String[HAS_ROI.size()]);
			}
		};
		registerConfigurableParameter(hasROIParam);
		hasROIParam.setExpression(HAS_ROI.get(0));

		boxROITypeParam = new StringParameter(this,"Type of Rectangular ROI") {
			private static final long serialVersionUID = -380964888849739100L;
			public String[] getChoices() {
				return BOX_ROI_TYPE.toArray(new String[BOX_ROI_TYPE.size()]);
			}
		};
		registerConfigurableParameter(boxROITypeParam);
		boxROITypeParam.setExpression(BOX_ROI_TYPE.get(0));
		
		//set the expression mode parameter to Evaluate after all data received
		passModeParameter.setExpression(EXPRESSION_MODE.get(1));
		
	}

	@Override
	protected DataMessageComponent getTransformedMessage(
			List<DataMessageComponent> cache) throws ProcessingException {
		
		final String plotName = plotViewName.getExpression();
		final String dataName = dataNameParam.getExpression();
		final String xaxisName = xaxisNameParam.getExpression();
		final String yaxisName = yaxisNameParam.getExpression();
		final String hasROISelection = hasROIParam.getExpression();
		final String boxTypeROI = boxROITypeParam.getExpression();
//		final List<IDataset>  data = MessageUtils.getDatasets(cache);
		final Map<String, Serializable>  data = MessageUtils.getList(cache);
		final DataMessageComponent mc = new DataMessageComponent();
		
		//add datasets to mc
		Set<String> dataKeys = data.keySet();
		for (String key : dataKeys) {
			AbstractDataset myData = ((AbstractDataset)data.get(key));
			mc.addList(myData.getName(), myData);
		}
		//mc.addList(data.get(0).getName(), (AbstractDataset)data.get(0));
		try {
			
			// open the plot view
			AbstractPlottingSystem plottingSystem = PlottingFactory.getPlottingSystem(plotName);
			
			int[] maxPos = ((AbstractDataset)data.get(dataName)).maxPos();
			double width = maxPos[0];
			double height = maxPos[1];
			ROIBase myROI = new RectangularROI(width, height/2, 0);
			
			if(xaxisName.equals("")||(yaxisName.equals("")))
				SDAPlotter.imagePlot(plotName, (AbstractDataset)data.get(dataName));
			else
				SDAPlotter.imagePlot(plotName, ((AbstractDataset)data.get(xaxisName)), ((AbstractDataset)data.get(yaxisName)), ((AbstractDataset)data.get(dataName)));
			
			// We plot the data to the image plot
	//		SDAPlotter.imagePlot(plotName, data.get(0));
			//GuiBean bean = SDAPlotter.getGuiBean(plotName);
			//if(bean!=null)
			//	System.out.println("BEAN:"+bean.toString());
			
			if(hasROISelection.equals(HAS_ROI.get(0))){
				//Create Region(s)
				Map<String, ROIBase> rois = MessageUtils.getROIs(cache);
				Set<Map.Entry<String, ROIBase>> roisSet = rois.entrySet();
				if(!roisSet.isEmpty()){
					Iterator<Entry<String, ROIBase>> it = roisSet.iterator();
					while(it.hasNext()){
						Entry<String,ROIBase> entry = it.next();
						String roiname = entry.getKey();
						myROI = entry.getValue();
						createRegion(plottingSystem, myROI, roiname, boxTypeROI);
						mc.addROI(roiname, myROI);
					}
				} else {
					createRegion(plottingSystem, myROI, "Default ROI", boxTypeROI);
					mc.addROI( "Default ROI", myROI);
				}
			}
			return mc;
		} catch (Exception e) {
			throw createDataMessageException("Displaying data sets", e);
		}
	}

	private void createRegion(final AbstractPlottingSystem plottingSystem, final ROIBase roi, final String roiName, final String boxType){
		Display.getDefault().asyncExec(new Runnable() {
			
			@Override
			public void run() {
				try {
					if(roi instanceof LinearROI){
						LinearROI lroi = (LinearROI)roi;
						IRegion region = plottingSystem.getRegion(roiName);
						if(region!=null&&region.isVisible()){
							region.setROI(lroi);
						}else {
							IRegion newRegion = plottingSystem.createRegion(roiName, RegionType.LINE);
							newRegion.setROI(lroi);
							plottingSystem.addRegion(newRegion);
							plottingSystem.setToolVisible("org.dawb.workbench.plotting.tools.boxProfileTool",
									ToolPageRole.ROLE_2D, "org.dawb.workbench.plotting.views.toolPageView.2D");
						}
					}
					if(roi instanceof RectangularROI){
						RectangularROI rroi = (RectangularROI)roi;
						IRegion region = plottingSystem.getRegion(roiName);
						
						//Test if the region is already there and update the currentRegion
						if(region!=null&&region.isVisible()){
//							currentROI = region.getROI();
//							if(currentROI.getPointX()<upperX && currentROI.getPointY()<lowerY)
//								region.setROI(currentROI);
//							else
							region.setROI(rroi);
						}else {
							if(boxType.equals(BOX_ROI_TYPE.get(0))){
								IRegion newRegion = plottingSystem.createRegion(roiName, RegionType.BOX);
								newRegion.setROI(rroi);
								plottingSystem.addRegion(newRegion);
							}
							if(boxType.equals(BOX_ROI_TYPE.get(1))){
								IRegion newRegion = plottingSystem.createRegion(roiName, RegionType.XAXIS);
								newRegion.setROI(rroi);
								plottingSystem.addRegion(newRegion);
							}
							if(boxType.equals(BOX_ROI_TYPE.get(2))){
								IRegion newRegion = plottingSystem.createRegion(roiName, RegionType.YAXIS);
								newRegion.setROI(rroi);
								plottingSystem.addRegion(newRegion);
							}
							plottingSystem.setToolVisible("org.dawb.workbench.plotting.tools.boxProfileTool",
									ToolPageRole.ROLE_2D, "org.dawb.workbench.plotting.views.toolPageView.2D");
						}
					}
					if(roi instanceof SectorROI){
						SectorROI sroi = (SectorROI)roi;
						IRegion region = plottingSystem.getRegion(roiName);
						if(region!=null&&region.isVisible()){
							region.setROI(sroi);
						}else {
							IRegion newRegion = plottingSystem.createRegion(roiName, RegionType.SECTOR);
							newRegion.setROI(sroi);
							plottingSystem.addRegion(newRegion);
							plottingSystem.setToolVisible("org.dawb.workbench.plotting.tools.boxProfileTool",
									ToolPageRole.ROLE_2D, "org.dawb.workbench.plotting.views.toolPageView.2D");
						}
					}
					if(roi instanceof EllipticalROI){
						EllipticalROI eroi = (EllipticalROI)roi;
						IRegion region = plottingSystem.getRegion(roiName);
						if(region!=null&&region.isVisible()){
							region.setROI(eroi);
						}else {
							IRegion newRegion = plottingSystem.createRegion(roiName, RegionType.ELLIPSE);
							newRegion.setROI(eroi);
							plottingSystem.addRegion(newRegion);
							plottingSystem.setToolVisible("org.dawb.workbench.plotting.tools.boxProfileTool",
									ToolPageRole.ROLE_2D, "org.dawb.workbench.plotting.views.toolPageView.2D");
						}
					}
					if(roi instanceof EllipticalFitROI){
						EllipticalFitROI efroi = (EllipticalFitROI)roi;
						IRegion region = plottingSystem.getRegion(roiName);
						if(region!=null&&region.isVisible()){
							region.setROI(efroi);
						}else {
							IRegion newRegion = plottingSystem.createRegion(roiName, RegionType.ELLIPSEFIT);
							newRegion.setROI(efroi);
							plottingSystem.addRegion(newRegion);
							plottingSystem.setToolVisible("org.dawb.workbench.plotting.tools.boxProfileTool",
									ToolPageRole.ROLE_2D, "org.dawb.workbench.plotting.views.toolPageView.2D");
						}
					}
					if(roi instanceof PointROI){
						PointROI proi = (PointROI)roi;
						IRegion region = plottingSystem.getRegion(roiName);
						if(region!=null&&region.isVisible()){
							region.setROI(proi);
						}else {
							IRegion newRegion = plottingSystem.createRegion(roiName, RegionType.POINT);
							newRegion.setROI(proi);
							plottingSystem.addRegion(newRegion);
							plottingSystem.setToolVisible("org.dawb.workbench.plotting.tools.boxProfileTool",
									ToolPageRole.ROLE_2D, "org.dawb.workbench.plotting.views.toolPageView.2D");
						}
					}
					
				} catch (Exception e) {
					logger.error("Couldn't open histogram view and create ROI", e);
				}
			}
		});
	}

	@Override
	protected String getOperationName() {
		return "Image Plot Actor";
	}

}
/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.spectrum.ui.file;

import java.util.Collection;
import java.util.List;

import org.eclipse.dawnsci.analysis.api.dataset.IDataset;

public interface ISpectrumFile extends IContain1DData {

	public String getName();
	
	public Collection<String> getDataNames();
	
	public List<String> getPossibleAxisNames();
	
	public List<String> getMatchingDatasets(int size);
	
	public IDataset getDataset(String name);
	
	public IDataset getxDataset();
	
	public List<IDataset> getyDatasets();
	
	public List<String> getyDatasetNames();
	
	public String getxDatasetName();
	
	public void setxDatasetName(String name);
	
	public boolean contains(String datasetName);
	
	public void removeyDatasetName(String name);
	
	public void addyDatasetName(String name);
	
	public void plotAll();
	
	public void removeAllFromPlot();
	
	public String getLongName();
	
	public boolean isUsingAxis();
	
	public void setUseAxis(boolean useAxis);
	
	public void setSelected(boolean selected);
	
	public void setShowPlot(boolean show);
	
	public boolean isShowPlot();
	
	public boolean canBeSaved();
}

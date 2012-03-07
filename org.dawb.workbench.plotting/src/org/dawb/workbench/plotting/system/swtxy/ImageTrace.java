package org.dawb.workbench.plotting.system.swtxy;

import org.csstudio.swt.xygraph.figures.Axis;
import org.csstudio.swt.xygraph.figures.IAxisListener;
import org.csstudio.swt.xygraph.linearscale.Range;
import org.dawb.common.services.IImageService;
import org.dawb.common.ui.image.PaletteFactory;
import org.dawb.common.ui.plot.region.RegionBounds;
import org.dawb.common.ui.plot.trace.IImageTrace;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.ui.PlatformUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;

/**
 * A trace which draws an image to the plot.
 * 
 * @author fcp94556
 *
 */
public class ImageTrace extends Figure implements IImageTrace, IAxisListener {
	
	private static final Logger logger = LoggerFactory.getLogger(ImageTrace.class);

	private String name;
	private Axis   xAxis;
	private Axis   yAxis;
	private AbstractDataset  image;
	private PaletteData paletteData;

	private int xStart, yStart;
	
	public ImageTrace(final String name, 
			          final Axis xAxis, 
			          final Axis yAxis,
			          final AbstractDataset unreflectedImage) {
		
		this.name  = name;
		this.xAxis = xAxis;
		
		// The image is drawn low y to the top left but the axes are low y to the bottom right
		// We do not currently reflect it as it takes too long. Instead in the slice
		// method, we allow for the fact that the dataset is in a different orientation to 
		// what is plotted.
		this.image = unreflectedImage;
		
		this.yAxis = yAxis;

		this.paletteData = PaletteFactory.getPalette();
		
		xAxis.setRange(0, unreflectedImage.getShape()[0]);
		yAxis.setRange(0, unreflectedImage.getShape()[1]);
		xAxis.addListener(this);
		yAxis.addListener(this);
		
		// TODO x-axis to be told to keep aspect ratio with y
		
		xStart = 0;
		yStart = 0;
	}
	
	private AbstractDataset getReflection(final AbstractDataset unreflectedImage) {
		
		final int[] shape = unreflectedImage.getShape();
		AbstractDataset tdata = AbstractDataset.zeros(shape, AbstractDataset.getDType(unreflectedImage));		
		for (int row = 0; row<shape[0]; ++row) {
			int newRow = shape[0]-row-1;
			for (int col = 0; col<shape[1]; ++col) {				
				Object val = unreflectedImage.getObject(row, col);
				tdata.set(val, newRow, col);
			}
		}
		return tdata;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Axis getxAxis() {
		return xAxis;
	}

	public void setxAxis(Axis xAxis) {
		this.xAxis = xAxis;
	}

	public Axis getyAxis() {
		return yAxis;
	}

	public void setyAxis(Axis yAxis) {
		this.yAxis = yAxis;
	}

	public AbstractDataset getImage() {
		return image;
	}

	public void setImage(AbstractDataset image) {
		this.image = image;
	}

	public PaletteData getPaletteData() {
		return paletteData;
	}

	public void setPaletteData(PaletteData paletteData) {
		this.paletteData = paletteData;
		createScaledImage();
		repaint();
	}

	
	private Image scaledImage;
	private Image rawImage;
	private Range xRangeCached, yRangeCached;
	
	/**
	 * Whenever this is called the SWT image is created
	 * and saved in the swtImage field.
	 */
	private void createScaledImage() {
		
		
		final Range xRange = xAxis.getRange();
		final Range yRange = yAxis.getRange();
				
		xStart     = xAxis.getValuePosition(xRange.getLower(), false);
		int xEnd   = xAxis.getValuePosition(xRange.getUpper(), false);
        
		yStart     = yAxis.getValuePosition(yRange.getUpper(), false);
		int yEnd   = yAxis.getValuePosition(yRange.getLower(), false);
		
		
		int width   = xEnd-xStart;
		int height  = yEnd-yStart;		
		
		final boolean isSameRange = (xRangeCached!=null && xRangeCached.equals(xRange) && yRangeCached!=null && yRangeCached.equals(yRange));
		
		if (!isSameRange || rawImage==null) {
			final RegionBounds regionBounds = new RegionBounds(new double[]{xRange.getLower(), yRange.getLower()}, 
	                                                           new double[]{xRange.getUpper(), yRange.getUpper()});
			AbstractDataset slice = slice(regionBounds);
			
			final IImageService service = (IImageService)PlatformUI.getWorkbench().getService(IImageService.class);
			try {
				this.rawImage   = service.getImage(slice, getPaletteData());
			} catch (Exception e) {
				logger.error("Cannot create image from data!", e);
			}
		}
		
		
		ImageData data = rawImage.getImageData().scaledTo(width, height);
		this.scaledImage = new Image(rawImage.getDevice(), data);
		
		xRangeCached = xRange;
		yRangeCached = yRange;
	}
	
	@Override
	protected void paintFigure(Graphics graphics) {
		
		if (scaledImage==null) createScaledImage();
		
		super.paintFigure(graphics);
		graphics.pushState();	
		graphics.drawImage(scaledImage, xStart, yStart);
		graphics.popState();
	}

	public void remove() {
		if (scaledImage!=null) scaledImage.dispose();
		getParent().remove(this);
		xAxis.removeListenr(this);
		yAxis.removeListenr(this);

	}

	@Override
	public AbstractDataset getData() {
		return image;
	}

	private RegionBounds imageBounds;
	@Override
	public AbstractDataset slice(RegionBounds bounds) {
		
		final AbstractDataset data = getData();
		
		// Check that a slice needed, this speeds up the initial show of the image.
		if (imageBounds==null) imageBounds = new RegionBounds(new double[]{0d, 0d},
				                                              new double[]{image.getShape()[0], image.getShape()[0]}); 
		if (imageBounds.equals(bounds))  return image;
	
		
		int[] xRange = getRange(bounds, 0, false);
		int[] yRange = getRange(bounds, 1, true);		
		
				
		try {
			return data.getSlice(new int[]{xRange[0],yRange[0]}, 
					             new int[]{xRange[1],yRange[1]}, 
					             new int[]{1,        1});
			
		}catch (IllegalArgumentException iae) {
			logger.error("Cannot slice image", iae);
			return getData();
		}
	}

	private int[] getRange(RegionBounds bounds, int index, boolean inverted) {
		
		final int side = image.getShape()[index];
		int start = (int)Math.round(bounds.getP1()[index]);
		if (inverted) start = side-start;
		
		int stop  = (int)Math.round(bounds.getP2()[index]);
		if (inverted) stop = side-stop;

		if (start>stop) {
			start = (int)Math.round(bounds.getP2()[index]);
			if (inverted) start = side-start;
			
			stop  = (int)Math.round(bounds.getP1()[index]);
			if (inverted) stop = side-stop;
		}
		
		if (start<0) start = 0;
		if (stop>image.getShape()[index]) stop = image.getShape()[index];

		
		return new int[]{start, stop};
	}

	@Override
	public void axisRangeChanged(Axis axis, Range old_range, Range new_range) {
		createScaledImage();
	}

	@Override
	public void axisRevalidated(Axis axis) {
		createScaledImage();
	}

	public void performAutoscale() {
		xAxis.setRange(0, image.getShape()[0]);
		yAxis.setRange(0, image.getShape()[1]);		
	}
}

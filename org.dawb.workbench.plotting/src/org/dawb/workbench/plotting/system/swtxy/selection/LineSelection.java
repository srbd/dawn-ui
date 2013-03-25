package org.dawb.workbench.plotting.system.swtxy.selection;

import java.util.Arrays;

import org.csstudio.swt.xygraph.figures.Axis;
import org.dawb.workbench.plotting.system.swtxy.translate.FigureTranslator;
import org.dawb.workbench.plotting.system.swtxy.util.Draw2DUtils;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FigureListener;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.UpdateManager;
import org.eclipse.draw2d.geometry.Geometry;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;

import uk.ac.diamond.scisoft.analysis.roi.LinearROI;
import uk.ac.diamond.scisoft.analysis.roi.ROIBase;

/**                 startBox2 (if isCrossHair()) 
 *                      |
 *                      |
 *                      |
 *                      |
 *                      |
 *    startBox-----------------------endBox
 *                      |
 *                      |
 *                      |
 *                      |
 *                      |
 *                   endBox2 (if isCrossHair())
 * 
 * @author fcp94556
 *
 */
class LineSelection extends AbstractSelectionRegion {

	private static final int SIDE      = 8;
	
	private SelectionHandle endBox, startBox;
	private SelectionHandle endBox2 = null, startBox2 = null;
	private Figure connection;

	LineSelection(String name, Axis xAxis, Axis yAxis) {
		super(name, xAxis, yAxis);
		setRegionColor(ColorConstants.cyan);
		setAlpha(80);
		setLineWidth(2);
	}

	@Override
	public void createContents(final Figure parent) {
				
		startBox = new RectangularHandle(xAxis, yAxis, getRegionColor(), parent, SIDE, 100, 100);
		FigureTranslator mover = new FigureTranslator(getXyGraph(), startBox);
		mover.addTranslationListener(createRegionNotifier());

		endBox = new RectangularHandle(xAxis, yAxis, getRegionColor(), parent, SIDE, 200, 200);
		mover = new FigureTranslator(getXyGraph(), endBox);	
		mover.addTranslationListener(createRegionNotifier());

		this.connection = new RegionFillFigure(this) {
			PointList shape = new PointList(4);

			@Override
			public void paintFigure(Graphics gc) {
				super.paintFigure(gc);
				final Point startCenter = startBox.getSelectionPoint();
				final Point endCenter   = endBox.getSelectionPoint();
				Point start2=null, end2=null;
				if (startBox2 != null) {
					start2 = startBox2.getSelectionPoint();
					end2 = endBox2.getSelectionPoint();
				}
				
				if (shape.size() == 0) {
					shape.addPoint(startCenter);
					shape.addPoint(endCenter);
					if (startBox2 != null) {
						shape.addPoint(start2);
						shape.addPoint(end2);
					}
				} else {
					shape.setPoint(startCenter, 0);
					shape.setPoint(endCenter, 1);
					if (startBox2 != null) {
						shape.setPoint(start2, 2);
						shape.setPoint(end2, 3);
					}
				}

				this.bounds = getConnectionBounds();
				gc.setLineWidth(getLineWidth());
				gc.setAlpha(getAlpha());
				gc.drawLine(startCenter, endCenter);
				if (start2!= null && end2 != null)
					gc.drawLine(start2, end2);
				LineSelection.this.drawLabel(gc, bounds);
				if (label != null) {
					label.setLocation(new Point(startCenter));
				}
			}

			@Override
			public boolean containsPoint(int x, int y) {
				if (!super.containsPoint(x, y)) return false;
				return Geometry.polylineContainsPoint(shape, x, y, 2);
			}
		};


		connection.setCursor(Draw2DUtils.getRoiMoveCursor());
		connection.setForegroundColor(getRegionColor());
		connection.setBounds(getConnectionBounds()); 
		connection.setOpaque(false);
		
		parent.add(connection);
		parent.add(startBox);
		parent.add(endBox);
		if (label != null)
			parent.add(label);
				
		final FigureListener figListener = createFigureListener();
		startBox.addFigureListener(figListener);
		endBox.addFigureListener(figListener);
		
		mover = new FigureTranslator(getXyGraph(), parent, connection, Arrays.asList(new IFigure[]{startBox,endBox}));
		mover.addTranslationListener(createRegionNotifier());
		if (label != null)
			setRegionObjects(connection, startBox, endBox, label);
		else
			setRegionObjects(connection, startBox, endBox);
		sync(getBean());
        updateROI();
        if (roi == null) createROI(true);
	}
	
	@Override
	public boolean containsPoint(double x, double y) {
		
		final int xpix = xAxis.getValuePosition(x, false);
		final int ypix = yAxis.getValuePosition(y, false);
		return connection.containsPoint(xpix, ypix);
	}


	@Override
	public void paintBeforeAdded(final Graphics gc, PointList clicks, Rectangle parentBounds) {
		gc.setLineStyle(Graphics.LINE_DOT);
		gc.setLineWidth(2);
		gc.setAlpha(getAlpha());
		gc.drawLine(clicks.getFirstPoint(), clicks.getLastPoint());
		if (startBox2!= null && endBox2 != null)
			gc.drawLine(startBox2.getSelectionPoint(), endBox2.getSelectionPoint());
	}

	@Override
	protected String getCursorPath() {
		return "icons/Cursor-line.png";
	}

	protected FigureListener createFigureListener() {
		return new FigureListener() {		
			@Override
			public void figureMoved(IFigure source) {				
				connection.repaint();
 			}
		};
	}

	@Override
	public ROIBase createROI(boolean recordResult) {
		if (startBox != null) {
			final double[] p1 = startBox.getPosition();
			final double[] p2 = endBox.getPosition();
			final LinearROI lroi = new LinearROI(p1, p2);
			if (recordResult)
				roi = lroi;
			return lroi;
		}
		return super.getROI();
	}
	
	@Override
	protected void updateROI(ROIBase roi) {
		if (roi instanceof LinearROI) {
			LinearROI lroi = (LinearROI) roi;
			if (startBox != null)
				startBox.setPosition(lroi.getPoint());
			if (endBox != null)
				endBox.setPosition(lroi.getEndPoint());
			
			if (startBox != null && lroi.isCrossHair() && (startBox2 == null || endBox2 == null) ) {
				startBox2 = new RectangularHandle(xAxis, yAxis, getRegionColor(), (Figure)connection.getParent(), SIDE, 100, 100); //start2x, start2y);
				endBox2 = new RectangularHandle(xAxis, yAxis, getRegionColor(), (Figure)connection, SIDE, 200, 200); // end2x, end2y);
				
				if (!connection.getParent().getChildren().contains(startBox2)) {
					connection.getParent().add(startBox2);
				}
				if (!connection.getParent().getChildren().contains(endBox2)) {
					connection.getParent().add(endBox2);
				}
				if (label != null)
					setRegionObjects(connection, startBox, endBox, startBox2, endBox2, label);
				else
					setRegionObjects(connection, startBox, endBox, startBox2, endBox2);
				
				FigureTranslator mover = new FigureTranslator(getXyGraph(), startBox2);
				mover.addTranslationListener(createRegionNotifier());

				mover = new FigureTranslator(getXyGraph(), endBox2);
				mover.addTranslationListener(createRegionNotifier());
			}
			if (!lroi.isCrossHair()) {
				startBox2 = null;
				endBox2 = null;
			}

			if (startBox2 != null)
				startBox2.setPosition(lroi.getPerpendicularBisectorPoint(0.0));
			if (endBox2 != null)
				endBox2.setPosition(lroi.getPerpendicularBisectorPoint(1.0));
			
			updateConnectionBounds();
		}
	}
	
	@Override
	protected void updateConnectionBounds() {
		if (connection==null) return;
		final Rectangle bounds = getConnectionBounds();
		
		UpdateManager updateMgr = null;
		try {
			updateMgr = connection.getParent()!=null
	                  ? connection.getParent().getUpdateManager()
	                  : connection.getUpdateManager();
		} catch (Throwable ignored) {
			// We intentionally allow the code to continue without the UpdateManager
		}

		if (label != null && labeldim != null) {
			Point pos1 = label.getLocation();
			Point pos2 = new Point(pos1.x + labeldim.width + 10, pos1.y + labeldim.height + 10);
			Rectangle r = new Rectangle(pos1, pos2);
			label.setBounds(r);
			if (updateMgr!=null) updateMgr.addDirtyRegion(label, r);
		}
		
		connection.setBounds(bounds);
		if (updateMgr!=null) updateMgr.addDirtyRegion(connection, bounds);
	}
	
	private static final int MIN_BOUNDS = 5;
	/**
	 * Ensures that very thin line does not stop connection moving
	 * @return
	 */
	private Rectangle getConnectionBounds() {
		Rectangle bounds  = null;
		if (startBox2 == null || endBox2 == null) {
			final Point startCenter = startBox.getSelectionPoint();
			final Point endCenter   = endBox.getSelectionPoint();
			bounds  = new Rectangle(startCenter, endCenter);
		}
		else {
			final int startx = Math.min(Math.min(startBox.getSelectionPoint().x, startBox2.getSelectionPoint().x),
					Math.min(endBox.getSelectionPoint().x, endBox2.getSelectionPoint().x));

			final int starty = Math.min(Math.min(startBox.getSelectionPoint().y, startBox2.getSelectionPoint().y),
					Math.min(endBox.getSelectionPoint().y, endBox2.getSelectionPoint().y));

			final int endx = Math.max(Math.max(startBox.getSelectionPoint().x, startBox2.getSelectionPoint().x),
					Math.max(endBox.getSelectionPoint().x, endBox2.getSelectionPoint().x));

			final int endy = Math.max(Math.max(startBox.getSelectionPoint().y, startBox2.getSelectionPoint().y),
					Math.max(endBox.getSelectionPoint().y, endBox2.getSelectionPoint().y));

			bounds  = new Rectangle(new Point(startx, starty), new Point(endx, endy));
		}
			
		if (bounds.height<MIN_BOUNDS) bounds.height=MIN_BOUNDS;
		if (bounds.width <MIN_BOUNDS) bounds.width=MIN_BOUNDS;
		return bounds;
	}

	/**
	 * Sets the local in local coordinates
	 * @param bounds
	 */
	@Override
	public void setLocalBounds(PointList clicks, Rectangle parentBounds) {
		if (startBox!=null)   startBox.setSelectionPoint(clicks.getFirstPoint());
		if (endBox!=null)     endBox.setSelectionPoint(clicks.getLastPoint());
		updateConnectionBounds();
		createROI(true);
		fireROIChanged(getROI());
	}

	@Override
	public RegionType getRegionType() {
		return RegionType.LINE;
	}

	@Override
	public int getMaximumMousePresses() {
		return 2;
	}
	
	@Override
	public void setLabel(Label label) {
		if (connection != null) {
			if (connection.getParent() != null) {
				if (this.label != null)
					connection.getParent().remove(this.label);
				connection.getParent().add(label);
			}
		}
		super.setLabel(label);
		label.setLabelAlignment(PositionConstants.LEFT);
		label.setTextAlignment(PositionConstants.LEFT);

		if (startBox2 != null && endBox2 != null)
			setRegionObjects(connection, startBox, endBox, startBox2, endBox2, label);
		else
			setRegionObjects(connection, startBox, endBox, label);
	}
}
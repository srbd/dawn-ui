package org.dawnsci.plotting.api.views;

import org.dawnsci.plotting.api.IPlottingSystem;
import org.dawnsci.plotting.api.PlottingFactory;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

/**
 * This view shows a list of registered plotting systems
 * using their registered name.
 * @author fcp94556
 *
 */
public class PlottingSystemsView extends ViewPart {


	public static final String ID = "org.dawnsci.plotting.api.views.PlottingSystemsView"; //$NON-NLS-1$

	private TableViewer viewer;
	
	public PlottingSystemsView() {
	}

	/**
	 * Create contents of the view part.
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {
		
		
		Composite container = new Composite(parent, SWT.NONE);
		
		TableColumnLayout layout = new TableColumnLayout();
		container.setLayout(layout);
		
		this.viewer = new TableViewer(container, SWT.FULL_SELECTION | SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		viewer.getTable().setLinesVisible(true);
		viewer.getTable().setHeaderVisible(true);
		createColumns(viewer, layout);
		
		viewer.setContentProvider(new IStructuredContentProvider() {
			@Override
			public void dispose() {
			}
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}

			@Override
			public Object[] getElements(Object inputElement) {
				final IPlottingSystem[] ps = PlottingFactory.getPlottingSystems();
				return ps!=null ? ps : new IPlottingSystem[]{};
			}
		});
		viewer.setInput(new Object());

		
		createActions();
		initializeToolBar();
		initializeMenu();
	}

	private void createColumns(TableViewer viewer, TableColumnLayout layout) {
		
       
		final TableViewerColumn system   = new TableViewerColumn(viewer, SWT.LEFT, 0);
		system.getColumn().setText("Registered Name");
		layout.setColumnData(system.getColumn(), new ColumnWeightData(100));
		system.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
	            return ((IPlottingSystem)element).getPlotName();
			}
		});
		
		final TableViewerColumn part   = new TableViewerColumn(viewer, SWT.LEFT, 1);
		part.getColumn().setText("Part Name");
		layout.setColumnData(part.getColumn(), new ColumnWeightData(100));
		part.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				IWorkbenchPart part = ((IPlottingSystem)element).getPart();
				if (part!=null) return part.getTitle();
				return null;
			}
		});


	}


	/**
	 * Create the actions.
	 */
	private void createActions() {
		getViewSite().getActionBars().getToolBarManager().add(new Action("Refresh") {
			public void run() {
				viewer.refresh();
			}
		});
	}

	/**
	 * Initialize the toolbar.
	 */
	private void initializeToolBar() {
		IToolBarManager toolbarManager = getViewSite().getActionBars()
				.getToolBarManager();
	}

	/**
	 * Initialize the menu.
	 */
	private void initializeMenu() {
		IMenuManager menuManager = getViewSite().getActionBars()
				.getMenuManager();
	}

	@Override
	public void setFocus() {
		viewer.getTable().setFocus();
	}

}

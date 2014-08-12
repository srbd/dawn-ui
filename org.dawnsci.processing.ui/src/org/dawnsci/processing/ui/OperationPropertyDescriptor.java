package org.dawnsci.processing.ui;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import org.dawb.common.util.list.ListUtils;
import org.dawb.common.util.text.StringUtils;
import org.dawnsci.common.widgets.celleditor.CComboCellEditor;
import org.dawnsci.common.widgets.decorator.BoundsDecorator;
import org.dawnsci.common.widgets.decorator.FloatArrayDecorator;
import org.dawnsci.common.widgets.decorator.FloatDecorator;
import org.dawnsci.common.widgets.decorator.IntegerArrayDecorator;
import org.dawnsci.common.widgets.decorator.IntegerDecorator;
import org.dawnsci.plotting.roi.RegionCellEditor;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.views.properties.PropertyDescriptor;

import uk.ac.diamond.scisoft.analysis.processing.model.IOperationModel;
import uk.ac.diamond.scisoft.analysis.processing.model.OperationModelField;
import uk.ac.diamond.scisoft.analysis.roi.IROI;

public class OperationPropertyDescriptor extends PropertyDescriptor implements Comparable<OperationPropertyDescriptor> {

	private IOperationModel model;
	private String          name;
	private ILabelProvider labelProvider;

	public OperationPropertyDescriptor(IOperationModel model, String name) {
		super(name, getDisplayName(model, name));
		this.model = model;
		this.name  = name;
	}
	
	private static OperationModelField getAnnotation(IOperationModel model, String fieldName) {
		
		try {
			Field field = getField(model, fieldName);
	        if (field!=null) {
	        	OperationModelField anot = field.getAnnotation(OperationModelField.class);
	        	if (anot!=null) {
	        		return anot;
	        	}
	        }
	        return null;
	        
		} catch (NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
			return null;
		}
		
	}
	
    private static Field getField(IOperationModel model, String fieldName) throws NoSuchFieldException, SecurityException {
		
    	Field field;
		try {
			field = model.getClass().getDeclaredField(fieldName);
		} catch (Exception ne) {
			field = model.getClass().getSuperclass().getDeclaredField(fieldName);
		}
		return field;
	}

	private static String getDisplayName(IOperationModel model, String fieldName) {
    	
    	OperationModelField anot = getAnnotation(model, fieldName);
    	if (anot!=null) {
    		String label = anot.label();
    		if (label!=null && !"".equals(label)) return label;
    	}
    	return fieldName;
	}

	public ILabelProvider getLabelProvider() {
        if (labelProvider != null) {
			return labelProvider;
		}
		return new LabelProvider();
    }


    public CellEditor createPropertyEditor(Composite parent) {
    	
        Object value;
		try {
			value = model.get(name);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		Class<? extends Object> clazz = null;
		if (value!=null) {
			clazz = value.getClass();
		} else {
			try {
				Field field = getField(model, name);
				clazz = field.getType();
			} catch (NoSuchFieldException | SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
        
        if (clazz == Boolean.class) {
        	return new CheckboxCellEditor(parent, SWT.NONE);
        	
        } else if (Number.class.isAssignableFrom(clazz) || isNumberArray(clazz)) {        	
        	return getNumberEditor(clazz, parent);
        	
        } else if (IROI.class.isAssignableFrom(clazz)) {        	
        	return new RegionCellEditor(parent);
        	
        } else if (Enum.class.isAssignableFrom(clazz)) {
        	return getChoiceEditor((Class<? extends Enum>)clazz, parent);
        
        }
        
        
        return null;
    }

	private boolean isNumberArray(Class<? extends Object> clazz) {
		
		if (clazz==null)      return false;
		if (!clazz.isArray()) return false;
		
		return double[].class.isAssignableFrom(clazz) || float[].class.isAssignableFrom(clazz) ||
               int[].class.isAssignableFrom(clazz)    || long[].class.isAssignableFrom(clazz);
	}

	private CellEditor getChoiceEditor(final Class<? extends Enum> clazz, Composite parent) {
		
		final Enum[]   values = clazz.getEnumConstants();
	    final String[] items  = Arrays.toString(values).replaceAll("^.|.$", "").split(", ");
		
		CComboCellEditor cellEd = new CComboCellEditor(parent, items) {
    	    protected void doSetValue(Object value) {
                if (value instanceof Enum) value = ((Enum) value).ordinal();
                super.doSetValue(value);
    	    }
    		protected Object doGetValue() {
    			Integer ordinal = (Integer)super.doGetValue();
    			return values[ordinal];
    		}
		};
		
		return cellEd;
	}

	private CellEditor getNumberEditor(final Class<? extends Object> clazz, Composite parent) {
    	
		final boolean isFloat      = Double.class.isAssignableFrom(clazz)   || Float.class.isAssignableFrom(clazz);
		final boolean isFloatArray = double[].class.isAssignableFrom(clazz) || float[].class.isAssignableFrom(clazz);
		
		final boolean isInt        = Integer.class.isAssignableFrom(clazz)  || Long.class.isAssignableFrom(clazz);
		final boolean isIntArray   = int[].class.isAssignableFrom(clazz)    || long[].class.isAssignableFrom(clazz);
		
		
    	final TextCellEditor textEd = new TextCellEditor(parent, SWT.NONE) {
    	    protected void doSetValue(Object value) {
                if (value instanceof Number)    value = value.toString();
                if (clazz.isArray()) {
                	String returnVal = StringUtils.toString(value);
      				if (returnVal!=null && returnVal.startsWith("[")) returnVal = returnVal.substring(1);
      				if (returnVal!=null && returnVal.endsWith("]"))   returnVal = returnVal.substring(0,returnVal.length()-1);
      				value = returnVal;
                }
                if (value==null) return;
                super.doSetValue(value);
    	    }
    		protected Object doGetValue() {
    			
    			String stringValue = (String)super.doGetValue();
    			
    			if (stringValue==null || "".equals(stringValue)) return null;
    			stringValue = stringValue.trim();
    			
      			if (Double.class.isAssignableFrom(clazz))  return new Double(stringValue);
      			if (Float.class.isAssignableFrom(clazz))   return new Float(stringValue);
      			if (Integer.class.isAssignableFrom(clazz)) return new Integer(stringValue);
      			if (Long.class.isAssignableFrom(clazz))    return new Long(stringValue);
      			
      			if (clazz.isArray()) {
      				if (stringValue.startsWith("[")) stringValue = stringValue.substring(1);
      				if (stringValue.endsWith("]"))   stringValue = stringValue.substring(0,stringValue.length()-1);
      				final List<String> strVals = ListUtils.getList(stringValue);
      				return getPrimitiveArray(clazz, strVals);
      			}
      			
    			return stringValue;
    		}
    	};
    	
    	final Text           text   = (Text) textEd.getControl();
    	
    	BoundsDecorator deco = null;
    	if (isFloat) {
    		deco = new FloatDecorator(text);
    	} else if (isInt) {
    		deco = new IntegerDecorator(text);
    	} else if (isFloatArray) {
    		deco = new FloatArrayDecorator(text);
    	} else if (isIntArray) {
    		deco = new IntegerArrayDecorator(text);
    	}
    	
    	if (deco!=null) {
        	OperationModelField anot = getAnnotation(model, name);
            if (anot!=null) {
            	deco.setMaximum(anot.max());
            	deco.setMinimum(anot.min());
            }
    	} 
    	
    	return textEd;
	}

	/**
	 * Not fast or pretty...
	 * 
	 * @param number
	 * @param strVals
	 * @return
	 */
	protected Object getPrimitiveArray(Class<? extends Object> clazz, List<String> strVals) {
		
		if (strVals==null || strVals.isEmpty()) return null;
	
		Object array = null;
		if (double[].class.isAssignableFrom(clazz)) array = new double[strVals.size()];
		if (float[].class.isAssignableFrom(clazz))  array = new float[strVals.size()];
		if (int[].class.isAssignableFrom(clazz))    array = new int[strVals.size()];
		if (long[].class.isAssignableFrom(clazz))   array = new long[strVals.size()];
		
		for (int i = 0; i < strVals.size(); i++) {
			Object value = null;
			if (double[].class.isAssignableFrom(clazz)) value = Double.parseDouble(strVals.get(i));
			if (float[].class.isAssignableFrom(clazz))  value = Float.parseFloat(strVals.get(i));
			if (int[].class.isAssignableFrom(clazz))    value = Integer.parseInt(strVals.get(i));
			if (long[].class.isAssignableFrom(clazz))   value = Long.parseLong(strVals.get(i));
			
			Array.set(array, i, value);
		}
		return array;
	}

	private class LabelProvider extends BaseLabelProvider implements ILabelProvider {

		private Image ticked, unticked;
		/**
		 * Creates a new label provider.
		 */
		public LabelProvider() {
		}


		/**
		 * The <code>LabelProvider</code> implementation of this
		 * <code>ILabelProvider</code> method returns <code>null</code>.
		 * Subclasses may override.
		 */
		public Image getImage(Object element) {
			if (element instanceof Boolean) {
				if (ticked==null)   ticked   = Activator.getImageDescriptor("icons/ticked.png").createImage();
				if (unticked==null) unticked = Activator.getImageDescriptor("icons/unticked.gif").createImage();
				Boolean val = (Boolean)element;
				return val ? ticked : unticked;
			}
			return null;
		}

		/**
		 * The <code>LabelProvider</code> implementation of this
		 * <code>ILabelProvider</code> method returns the element's
		 * <code>toString</code> string. Subclasses may override.
		 */
		public String getText(Object element) {
			if (element instanceof Boolean) return "";
			if (element.getClass().isArray()) return StringUtils.toString(element);
			return element == null ? "" : element.toString();//$NON-NLS-1$
		}
		
		public void dispose() {
			if (ticked!=null)   ticked.dispose();
			if (unticked!=null) unticked.dispose();
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OperationPropertyDescriptor other = (OperationPropertyDescriptor) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public int compareTo(OperationPropertyDescriptor o) {
		return name.compareTo(o.name);
	}
	
	@Override
	public String toString() {
		return name;
	}
}
package org.webguitoolkit.ui.addons;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.webguitoolkit.ui.base.WebGuiFactory;
import org.webguitoolkit.ui.controls.IComposite;
import org.webguitoolkit.ui.controls.container.ICanvas;
import org.webguitoolkit.ui.controls.form.ICheckBox;
import org.webguitoolkit.ui.controls.form.ILabel;
import org.webguitoolkit.ui.controls.form.IText;
import org.webguitoolkit.ui.controls.layout.ITableBasedLayoutData;
import org.webguitoolkit.ui.controls.layout.SequentialTableLayout;
import org.webguitoolkit.ui.controls.table.ITable;
import org.webguitoolkit.ui.controls.table.ITableColumn;
import org.webguitoolkit.ui.controls.util.MasterDetailFactory;
import org.webguitoolkit.ui.controls.util.conversion.ConvertUtil.ConversionException;
import org.webguitoolkit.ui.controls.util.conversion.IConverter;

/**
 * This class provides functions to generate common WGT UI patterns consisting of multiple controls.
 * 
 * @author peter@17sprints.de
 * 
 */
public class UiPatternFactory {

	private WebGuiFactory factory;
	private int tableRows = 8;
	private String keyPrefix;
	private boolean columnFilter = true;
	private MasterDetailFactory mdf;
	private List<String> exclusionList;
	private String[] exclusionNames = new String[] { "getPersistenceUtility", "getLogInfo", "getObjectUId",
			"getPropertyLength", "getClass", "getKey", "getSession" };

	public UiPatternFactory(WebGuiFactory factory) {
		this.factory = factory;
		mdf = new MasterDetailFactory();
		exclusionList = Arrays.asList(exclusionNames);
	}

	/**
	 * Create a canvas with SequentialTableLayout and v-aligned to the top.
	 * 
	 * @param parent
	 *            where to create it
	 * @return the canvas
	 */
	public ICanvas createCanvasWithLayout(IComposite parent) {
		ICanvas result = factory.createCanvas(parent);
		result.setLayout(new SequentialTableLayout());
		result.setLayoutData(SequentialTableLayout.getLayoutData());
		ITableBasedLayoutData layoutData = (ITableBasedLayoutData) result.getLayoutData();
		layoutData.addCellAttribute("valign", "top");
		return result;
	}

	/**
	 * Uses the MasterDetailFactory to create a table. Adds a column for each attribute that has a get- or is-method.
	 * 
	 * @param clazz
	 *            the class whose attributes (get*, is* shall rendered as columns)
	 * @param viewConnector
	 *            the place where the table is linked in
	 * @param name
	 *            the tables id (NULL is OK)
	 * @param title
	 *            TODO
	 * @param mandatory
	 *            TODO
	 * @return the table with columns
	 */
	public ITable createTableForClass(Class clazz, ICanvas viewConnector, String name, String title, String[] ignore,
			String[] mandatory) {

		if (name != null && name.indexOf('.') != -1)
			throw new IllegalArgumentException("name must not contain e '.' : " + name);

		ICanvas canvas = factory.createCanvas(viewConnector);

		ITable table = factory.createTable(canvas, title, tableRows);
		table.setName(name);
		if (keyPrefix == null)
			keyPrefix = clazz.getSimpleName();

		List ignoredFields = (ignore != null) ? (Arrays.asList(ignore)) : (Collections.emptyList());
		List mandatoryFields = (mandatory != null) ? (Arrays.asList(mandatory)) : (Collections.emptyList());

		for (ColumnDescriptor col : createColumnDescriptors(clazz)) {
			if (!ignoredFields.contains(col.getName())) {
				ITableColumn column = factory.createTableColumn(table, col.getKey(), col.getName(), columnFilter);
				column.setMandatory(mandatoryFields.contains(col.getName()));
			}
		}

		return table;

	}

	/**
	 * Create a Text field with Label in a SequentialTableayout
	 * 
	 * @param location
	 *            the composite to put this in
	 * @param labelkey
	 *            the path for the labe
	 * @param property
	 *            the property to be accessed
	 * @param isLast
	 *            last int row?
	 * @param length
	 *            maximum length for the field
	 * @return the created text control
	 * 
	 */
	public IText createTextWithLabel(IComposite location, String labelkey, String property, boolean isLast, int length) {
		ILabel label = factory.createLabel(location, labelkey);
		label.setLabelForFormControl(true);
		IText text = factory.createText(location, property);
		text.setMaxlength(length);
		text.setConverter(new TrimConverter());
		if (isLast)
			text.setLayoutData(SequentialTableLayout.getLastInRow());
		return text;
	}

	public IText createNumericTextWithLabel(IComposite location, String labelkey, String property, boolean isLast,
			int length) {
		ILabel label = factory.createLabel(location, labelkey);
		label.setLabelForFormControl(true);
		IText text = factory.createText(location, property);
		text.setMaxlength(length);
		if (isLast)
			text.setLayoutData(SequentialTableLayout.getLastInRow());
		return text;
	}

	/**
	 * 
	 * @param location
	 * @param labelkey
	 * @param cssclass
	 * @param isLast
	 * @return
	 */
	public ILabel createLabelWithStyle(IComposite location, String labelkey, String cssclass, boolean isLast) {
		ILabel label = factory.createLabel(location, labelkey);
		if (isLast)
			label.setLayoutData(SequentialTableLayout.getLastInRow());
		label.addCssClass(cssclass);
		return label;
	}

	/**
	 * 
	 * @param clazz
	 *            the class which shall be displayed
	 * @param parent
	 *            where to connect to
	 * @param properties
	 *            a variable list of properties that shall be rendered on the canvas. Format: name,type,x,y
	 * @return
	 */
	public ICanvas createFormForClass(Class clazz, ICanvas parent, String... properties) {
		ICanvas resultCanvas = createCanvasWithLayout(parent);
		Map<String, FieldDescriptor> fieldDescriptors = createFieldDescriptors(clazz);
		for (String property : properties) {
			FieldDescriptor fd = fieldDescriptors.get(property);
			if (fd != null)
				createControl(fd, resultCanvas);
		}
		return resultCanvas;
	}

	/**
	 * 
	 * @param result
	 * @param property
	 *            Format: name,type,x,y,...
	 */
	private void createControl(FieldDescriptor fieldDescriptor, ICanvas result) {

		switch (fieldDescriptor.type) {
		case FieldDescriptor.DEFAULT: // create a text
			IText text = createTextWithLabel(result, fieldDescriptor.getKey(), fieldDescriptor.getName(), true, 32);
			text.setEditable(fieldDescriptor.isWriteable());
			break;
		case FieldDescriptor.BOOLEAN: // create a checkbox
			ILabel label = factory.createLabel(result, fieldDescriptor.getKey());
			label.setLabelForFormControl(true);
			ICheckBox checkbox = factory.createCheckBox(result, fieldDescriptor.getName());
			checkbox.setDescribingLabel(label);
			checkbox.setLayoutData(SequentialTableLayout.getLastInRow());
			break;
		default:
			label = factory.createLabel(result, "???" + fieldDescriptor.getKey());
		}
	}

	/**
	 * 
	 * @param clazz
	 * @return
	 */
	private List<ColumnDescriptor> createColumnDescriptors(Class clazz) {
		List<ColumnDescriptor> result = new ArrayList();
		Method[] declaredMethods = clazz.getMethods();
		for (int i = 0; i < declaredMethods.length; i++) {
			Method method = declaredMethods[i];
			String name = method.getName();
			int type;
			if (name.startsWith("get")) {
				if (exclusionList.contains(name))
					continue;
				name = name.substring("get".length());
				// respect camel case names
				name = name.substring(0, 1).toLowerCase().concat(name.substring(1));

				if (method.getReturnType().isAssignableFrom(Integer.class)
						|| method.getReturnType().isAssignableFrom(Float.class)
						|| method.getReturnType().isAssignableFrom(Long.class)
						|| method.getReturnType().isAssignableFrom(Double.class)) {
					type = ColumnDescriptor.NUMERIC;
				} else if (method.getReturnType().isAssignableFrom(Collection.class)) {
					name += ".size";
					type = ColumnDescriptor.NUMERIC;
				} else if (method.getReturnType().isAssignableFrom(Boolean.class)) {
					type = ColumnDescriptor.BOOLEAN;
				} else
					type = ColumnDescriptor.DEFAULT;
			} else if (name.startsWith("is")) {
				if (method.getReturnType().isAssignableFrom(Boolean.class)) {
					name = name.substring("is".length());
					name = name.substring(0, 1).toLowerCase().concat(name.substring(1));
					type = ColumnDescriptor.BOOLEAN;
				} else
					continue;
			} else
				continue;

			result.add(new ColumnDescriptor(keyPrefix + "." + name + "@*" + StringUtils.capitalize(name), name, type));

		}

		return result;
	}

	class ColumnDescriptor {

		static final int DEFAULT = 1;
		static final int BOOLEAN = 2;
		static final int NUMERIC = 3;

		private String key;
		private String name;
		private int type;

		ColumnDescriptor(String key, String name, int type) {
			this.key = key;
			this.type = type;
			this.name = name;
		}

		public String getKey() {
			return key;
		}

		public String getName() {
			return name;
		}

		public int getType() {
			return type;
		}

		public String toString() {
			return "ColumnDescriptor { " + getName() + ", " + getKey() + ", " + getType() + " }";
		}
	}

	/**
	 * 
	 * @param clazz
	 * @return
	 */
	private Map<String, FieldDescriptor> createFieldDescriptors(Class clazz) {
		Map<String, FieldDescriptor> result = new HashMap();
		Method[] declaredMethods = clazz.getMethods();
		// check the getter
		for (int i = 0; i < declaredMethods.length; i++) {
			Method method = declaredMethods[i];
			String name = method.getName();
			int type;
			if (name.startsWith("get")) {
				if (exclusionList.contains(name))
					continue;
				name = name.substring("get".length());
				// respect camel case names
				name = name.substring(0, 1).toLowerCase().concat(name.substring(1));

				if (method.getReturnType().isAssignableFrom(Integer.class)
						|| method.getReturnType().isAssignableFrom(Float.class)
						|| method.getReturnType().isAssignableFrom(Long.class)
						|| method.getReturnType().isAssignableFrom(Double.class)) {
					type = FieldDescriptor.NUMERIC;
				} else if (method.getReturnType().isAssignableFrom(Collection.class)) {
					name += ".size";
					type = FieldDescriptor.NUMERIC;
				} else if (method.getReturnType().isAssignableFrom(Boolean.class)) {
					type = FieldDescriptor.BOOLEAN;
				} else
					type = FieldDescriptor.DEFAULT;

			} else if (name.startsWith("is")) {
				if (method.getReturnType().isAssignableFrom(Boolean.class)) {
					name = name.substring("is".length());
					name = name.substring(0, 1).toLowerCase().concat(name.substring(1));
					type = FieldDescriptor.BOOLEAN;
				} else
					continue;
			} else
				continue;

			result.put(name, new FieldDescriptor(keyPrefix + "." + name + "@*" + StringUtils.capitalize(name), name,
					type));
		}

		// check the setters
		for (int i = 0; i < declaredMethods.length; i++) {
			Method method = declaredMethods[i];
			String name = method.getName();

			if (name.startsWith("set")) {
				if (exclusionList.contains(name))
					continue;
				name = name.substring("set".length());
				// respect camel case names
				name = name.substring(0, 1).toLowerCase().concat(name.substring(1));
				FieldDescriptor fieldDescriptor = result.get(name);
				if (fieldDescriptor != null)
					fieldDescriptor.setWriteable(true);
			} else
				continue;
		}
		System.out.println(result);
		return result;
	}

	/**
	 * 
	 * @author peter@17sprints.de
	 * 
	 */

	class FieldDescriptor {

		static final int DEFAULT = 1;
		static final int BOOLEAN = 2;
		static final int NUMERIC = 3;

		private String key;
		private String name;
		private int type;
		private boolean writeable;

		FieldDescriptor(String key, String name, int type) {
			this.key = key;
			this.type = type;
			this.name = name;
		}

		public String getKey() {
			return key;
		}

		public String getName() {
			return name;
		}

		public int getType() {
			return type;
		}

		public String toString() {
			return "FieldDescriptor { " + getName() + ", " + getKey() + ", " + getType()
					+ ((isWriteable() ? ("rw") : ("ro"))) + " }";
		}

		public void setWriteable(boolean writeable) {
			this.writeable = writeable;
		}

		public boolean isWriteable() {
			return writeable;
		}
	}

	public void setTableRows(int tableRows) {
		this.tableRows = tableRows;
	}

	public void setKeyPrefix(String keyPrefix) {
		this.keyPrefix = keyPrefix;
	}

	public void setColumnFilter(boolean columnFilter) {
		this.columnFilter = columnFilter;
	}

	public String getKeyPrefix() {
		return keyPrefix;
	}

	public MasterDetailFactory getMdf() {
		return mdf;
	}

	class TrimConverter implements IConverter {

		@Override
		public Object parse(String value) throws ConversionException {
			return value.trim();
		}

		@Override
		public Object convert(Class type, Object value) {
			if (type == String.class && value != null)
				return String.valueOf(value);
			return value;
		}

	}
}

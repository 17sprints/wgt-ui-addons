package org.webguitoolkit.ui.addons;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.webguitoolkit.ui.base.WebGuiFactory;
import org.webguitoolkit.ui.controls.IComposite;
import org.webguitoolkit.ui.controls.container.ICanvas;
import org.webguitoolkit.ui.controls.form.ILabel;
import org.webguitoolkit.ui.controls.form.IText;
import org.webguitoolkit.ui.controls.layout.GridLayout;
import org.webguitoolkit.ui.controls.layout.ITableBasedLayoutData;
import org.webguitoolkit.ui.controls.layout.SequentialTableLayout;
import org.webguitoolkit.ui.controls.table.ITable;
import org.webguitoolkit.ui.controls.util.MasterDetailFactory;

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
	 * @param id
	 *            the tables id (NULL is OK)
	 * @param title TODO
	 * @return the table with columns
	 */
	public ITable createTableForClass(Class clazz, ICanvas viewConnector, String id, String title) {

		if (id != null && id.indexOf('.') != -1)
			throw new IllegalArgumentException("id must not contain e '.' : " + id);
		ICanvas canvas = factory.createCanvas(viewConnector);

		ITable table = factory.createTable(canvas, title, tableRows, id);

		if (keyPrefix == null)
			keyPrefix = clazz.getSimpleName();

		for (ColumnDescriptor col : createColumnDescriptors(clazz)) {
			factory.createTableColumn(table, col.getKey(), col.getName(), columnFilter);
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
	 * @return the created text control
	 * @param isLast
	 *            last int row?
	 * @return the created text control
	 */
	public IText createTextWithLabel(IComposite location, String labelkey, String property, boolean isLast) {
		ILabel label = factory.createLabel(location, labelkey);
		label.setLabelForFormControl(true);
		IText text = factory.createText(location, property);
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
		ICanvas result = factory.createCanvas(parent);
		result.setLayout(new GridLayout());
		for (String property : properties) {
			createControl(result, property);
		}
		return result;
	}

	/**
	 * 
	 * @param result
	 * @param property
	 *            Format: name,type,x,y,...
	 */
	private void createControl(ICanvas result, String property) {
		String[] opts = StringUtils.split(property, ',');
		String name = opts[0];
		String type = opts[1].toUpperCase();
		int x = Integer.parseInt(opts[2]);
		int y = Integer.parseInt(opts[3]);
		switch (type.charAt(0)) {
		case 'L': // create a label
			break;
		case 'T': // create a text
			break;
		case 'C': // create a checkbox
			break;
		case 'B': // create a textbox
			break;
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
					type = ColumnDescriptor.NUMERIC;
				} else if (method.getReturnType().isAssignableFrom(Collection.class)) {
					name += ".size";
					type = ColumnDescriptor.NUMERIC;
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
				fieldDescriptor.setWriteable(true);
			} else
				continue;
		}
		return result;
	}

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
			return "ColumnDescriptor { " + getName() + ", " + getKey() + ", " + getType() + " }";
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
}

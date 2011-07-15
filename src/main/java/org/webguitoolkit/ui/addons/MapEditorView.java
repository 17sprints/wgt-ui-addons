package org.webguitoolkit.ui.addons;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.webguitoolkit.ui.base.DataBag;
import org.webguitoolkit.ui.base.IDataBag;
import org.webguitoolkit.ui.base.WebGuiFactory;
import org.webguitoolkit.ui.controls.AbstractView;
import org.webguitoolkit.ui.controls.container.ICanvas;
import org.webguitoolkit.ui.controls.event.ClientEvent;
import org.webguitoolkit.ui.controls.event.IActionListener;
import org.webguitoolkit.ui.controls.form.IButton;
import org.webguitoolkit.ui.controls.form.ICompound;
import org.webguitoolkit.ui.controls.form.IText;
import org.webguitoolkit.ui.controls.layout.SequentialTableLayout;

/**
 * Provides a editor for a key-value map of Strings. <br>
 * You can add and remove entries as well as edit existing ones. The model has to be provided as Map. The caller has to
 * embed method call into his (ButtonBar) listener in order to transfer the content from the map to the internal
 * Compound and vice-versa. <br>
 * Used CSS classes : <br>
 * "mevrembut" styles the remove button<br>
 * "mevaddbut" styles the add button <br>
 * "mevkeytext" styles the key text<br>
 * "mevvaltext" styles the value text<br>
 * "mevcanv" styles the canvas
 * 
 * @author peter@17sprints.de
 * 
 */
public class MapEditorView extends AbstractView {

	private static final long serialVersionUID = 7173822642048203379L;
	private static final String VAL_PREFIX = "val_";
	private static final String KEY_PREFIX = "key_";
	private Map<String, String> map;
	private DataBagWrapper bag;
	private ICompound compound;
	private Map<String, String> removeIndex;
	private IButton addButton;
	private List<IButton> removeButtons;
	private int componentMode = ICompound.MODE_READONLY;
	// private int width = 400, height = 250;
	private int nextIndex;

	public MapEditorView(WebGuiFactory factory, ICanvas viewConnector) {
		super(factory, viewConnector);
		bag = new DataBagWrapper(null);
	}

	@Override
	protected void createControls(WebGuiFactory factory, ICanvas viewConnector) {
		ICanvas canvas = factory.createCanvas(viewConnector);
		canvas.addCssClass("mevcanv");
		SequentialTableLayout layout = new SequentialTableLayout();
		compound = factory.createCompound(canvas);
		compound.setLayout(layout);

		if (getModel().size() == 0) {
			if (componentMode == ICompound.MODE_READONLY)
				factory.createLabel(compound, "no elements");
			else {
				bag.addProperty(KEY_PREFIX + 0, "{key-1}");
				bag.addProperty(VAL_PREFIX + 0, "{value}");
			}
			// componentMode = ICompound.MODE_EDIT;
		}
		compound.changeElementMode(componentMode);

		removeIndex = new HashMap<String, String>();
		removeButtons = new ArrayList<IButton>();
		List<String> keys = new ArrayList<String>();
		for (Iterator<String> it = bag.getProperties().keySet().iterator(); it.hasNext();) {
			String key = it.next();
			if (key.startsWith(VAL_PREFIX))
				continue;
			keys.add(key);
		}

		for (Iterator<String> it = keys.iterator(); it.hasNext();) {
			String key = it.next();
			final String index = key.substring(KEY_PREFIX.length());
			IText keyText = factory.createText(compound, key);
			keyText.addCssClass("mevkeytext");
			IText valueText = factory.createText(compound, VAL_PREFIX + index);
			valueText.addCssClass("mevvaltext");
			IButton removeButton = factory.createLinkButton(compound, "images/trash_small.gif", null,
					"Remove this entry", new IActionListener() {
						public void onAction(ClientEvent event) {
							String id = event.getSourceId();
							String bagkey = (String) removeIndex.get(id);
							// System.out.println("remove " + bagkey + " from " + bag.getProperties().keySet());
							bag.getProperties().remove(bagkey);
							String index = bagkey.substring(KEY_PREFIX.length());
							bag.getProperties().remove(VAL_PREFIX + index);
							compound.save();
							show();
						}
					});
			removeButton.addCssClass("mevrembut");
			removeButton.setTooltip("Remove entry ".concat(key));
			removeIndex.put(removeButton.getId(), key);
			removeButtons.add(removeButton);

			removeButton.setVisible(componentMode == ICompound.MODE_EDIT);

			if (!it.hasNext()) {
				addButton = factory.createLinkButton(compound, null, "add", "Add an entry", new IActionListener() {
					public void onAction(ClientEvent event) {
						compound.save();
						// System.out.println("add " + KEY_PREFIX + nextIndex + " to  " + bag.getProperties().keySet());
						bag.addProperty(KEY_PREFIX + nextIndex, "{new key}");
						bag.addProperty(VAL_PREFIX + nextIndex++, "{new value}");
						show();
					}
				});
				addButton.setLayoutData(SequentialTableLayout.getLastInRow());

				addButton.setVisible(componentMode == ICompound.MODE_EDIT);
				addButton.addCssClass("mevaddbut");
			} else
				removeButton.setLayoutData(SequentialTableLayout.getLastInRow());
		}
		compound.setBag(bag);
		compound.load();
	}

	/**
	 * load the model into the editors data bag
	 */
	public void load() {
		bag.clearProperties();
		Set<Entry<String, String>> entrySet = map.entrySet();
		int i = 0;
		for (Entry<String, String> entry : entrySet) {
			bag.addProperty(KEY_PREFIX + i, entry.getKey());
			bag.addProperty(VAL_PREFIX + i++, entry.getValue());
		}
		nextIndex = i + 1;
	}

	/**
	 * save the databag to the provided map.
	 */
	public void save2Model() {
		DataBagWrapper theBag = (DataBagWrapper) compound.getBag();
		map.clear();
		for (String key : theBag.getProperties().keySet()) {
			if (key.startsWith(KEY_PREFIX)) {
				String number = key.substring(KEY_PREFIX.length());
				String keyValue = theBag.getString(key);
				String value = theBag.getString(VAL_PREFIX + number);
				map.put(keyValue, value);
			}
		}
	}

	/**
	 * Set the data model as mal
	 * 
	 * @param map
	 */
	public void setModel(Map<String, String> map) {
		this.map = map;
	}

	public Map<String, String> getModel() {
		return map;
	}

	/**
	 * Call this from your listener in postSave(). Updates the model with the current state of the DataBag.
	 */
	public void postSaveAction() {
		save2Model();
		componentMode = ICompound.MODE_READONLY;
		if (bag.getProperties().size() > 0) {
			for (IButton button : removeButtons) {
				button.setVisible(false);
			}
			if (addButton != null)
				addButton.setVisible(false);
		}
		show();
	}

	/**
	 * Call this from your listener in postCancel(). Loads the models state into the internal DataBag and shows the view
	 * in READONLY mode.
	 */
	public void postCancelAction() {
		componentMode = ICompound.MODE_READONLY;
		load();
		show();
	}

	/**
	 * Call this from your listener in preEdit(). The action controls to add and delete entries are made visible and the
	 * internal compound will be set to MODE_EDIT.
	 */
	public void preEditAction() {
		if (bag.getProperties().size() > 0) {
			for (IButton button : removeButtons) {
				button.setVisible(true);
			}
			if (addButton != null)
				addButton.setVisible(true);
		}
		componentMode = ICompound.MODE_EDIT;
	}

	/**
	 * Provides access to the property map of DataBag
	 */
	private class DataBagWrapper extends DataBag implements IDataBag {

		public DataBagWrapper(Object delegate) {
			super(delegate);
		}

		public Map<String, Object> getProperties() {
			return properties;
		}

		public String toString() {
			StringBuffer result = new StringBuffer("DataBag { ");
			for (Entry<String, Object> entry : properties.entrySet()) {
				result.append(entry.getKey()).append("=").append(entry.getValue()).append(", ");
			}
			result.append(" }");
			return result.toString();
		}
	}

	public int getComponentMode() {
		return componentMode;
	}

	public void setComponentMode(int componentMode) {
		this.componentMode = componentMode;
	}
}

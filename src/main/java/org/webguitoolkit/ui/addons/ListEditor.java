package org.webguitoolkit.ui.addons;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
 * Provides a editor for a maintainable collection of Strings. <br>
 * You can add and remove entries as well as edit existing ones. The model has to be provided as List. The caller has to
 * embed method call into his (ButtonBar) listener in order to transfer the content from the map to the internal
 * Compound and vice-versa. <br>
 * CSS classes : <br>
 * "levrembut" styles the remove button<br>
 * "levaddbut" styles the add button <br>
 * "levvaltext" styles the value text<br>
 * "levcanv" styles the canvas<br>
 * <br>
 * Property keys:<br>
 * wgt.addons.listedit.noelements<br>
 * wgt.addons.listedit.remove.tooltip<br>
 * wgt.addons.listedit.add.tooltip<br>
 * wgt.addons.listedit.add.text<br>
 * <br>
 * Images:<br>
 * images/trash_small.gif
 * <br>
 * TODO:<br>
 * Add validators for key and value field
 * @author peter@17sprints.de
 * 
 */
public class ListEditor extends AbstractView {

	private static final long serialVersionUID = -7670911260039716037L;
	private static final String REMOVE_TOOLTIP_TEXT = "wgt.addons.listedit.remove.tooltip@Remove this entry";
	private static final String NO_ELEMENTS_TEXT = "wgt.addons.listedit.noelements@No elements";
	private static final String ADD_BUTTON_TOOLTIP = "wgt.addons.listedit.add.tooltip@Add an entry";
	private static final String ADD_BUTTON_TEXT = "wgt.addons.listedit.add.text@add";
	
	private static final String KEY_PREFIX = "key_";

	private Collection<String> model;
	private IDataBag bag;
	private ICompound compound;
	private HashMap<String, String> removeButton2keyMap;
	private IButton addButton;
	private List<IButton> removeButtons;
	private int componentMode = ICompound.MODE_READONLY;
	private int nextIndex = 0;
	private InternalModel internalModel = new InternalModel();

	public ListEditor(WebGuiFactory factory, ICanvas viewConnector) {
		super(factory, viewConnector);
		bag = new DataBag(internalModel);
	}

	@Override
	protected void createControls(WebGuiFactory factory, ICanvas viewConnector) {
		ICanvas canvas = factory.createCanvas(viewConnector);
		canvas.addCssClass("levcanv");
		SequentialTableLayout layout = new SequentialTableLayout();
		compound = factory.createCompound(canvas);
		compound.setBag(bag);
		compound.setLayout(layout);
		
		//TODO REVIEW : buttons im R/O mode weglassen ? ODER create controls vermeiden (wie)?
		IActionListener addButtonListener = new IActionListener() {
			
			private static final long serialVersionUID = -5289960859129358688L;

			public void onAction(ClientEvent event) {
				addBagProperty(null);
				compound.load();
				show();
			}
		};
		
		//TODO REVIE . serial id noetig?
		IActionListener removeButtonListener = new IActionListener() {
		
			private static final long serialVersionUID = -4293541107690135557L;

			public void onAction(ClientEvent event) {
				String id = event.getSourceId();
				String key = removeButton2keyMap.get(id);
				internalModel.getMap().remove(key);
				
				//removeButton2keyMap.remove(id);
				show();
			}
		};
		
		if (internalModel.getMap().size() == 0) {
			if (componentMode == ICompound.MODE_EDIT) {
				addButton = factory.createLinkButton(compound, null, ADD_BUTTON_TEXT,
						ADD_BUTTON_TOOLTIP, addButtonListener);
			} else
				factory.createLabel(compound, NO_ELEMENTS_TEXT);
			return;
		}

		removeButton2keyMap = new HashMap<String, String>();
		removeButtons = new ArrayList<IButton>();

		// create the ui elements depending on the bags entries

		//TODO REVIEW : zusammenspiel mit umgebenden compound usw.

		for (Iterator<Entry<String,String>> it = internalModel.getMap().entrySet().iterator(); it.hasNext();) {
			Entry<String,String> e = it.next();
			
			// value field
			IText valueText = factory.createText(compound, "map(" + e.getKey() + ")");
			valueText.addCssClass("levvaltext");
		
			// remove button
			IButton removeButton = factory.createLinkButton(compound, "images/trash_small.gif", null,
					REMOVE_TOOLTIP_TEXT, removeButtonListener);
			removeButton.addCssClass("levrembut");

			// keep the mapping from button to bag key to delete the right entry
			removeButton2keyMap.put(removeButton.getId(), e.getKey());

			// keep the button to show / hide them depending on componentMode
			removeButtons.add(removeButton);
			removeButton.setVisible(componentMode == ICompound.MODE_EDIT);

			if (!it.hasNext()) {
				addButton = factory.createLinkButton(compound, null, ADD_BUTTON_TEXT,
						ADD_BUTTON_TOOLTIP, addButtonListener);
				addButton.setLayoutData(SequentialTableLayout.getLastInRow());

				addButton.setVisible(componentMode == ICompound.MODE_EDIT);
				addButton.addCssClass("levaddbut");
			} else
				removeButton.setLayoutData(SequentialTableLayout.getLastInRow());
		}

		compound.changeElementMode(componentMode);
		compound.load();
	}

	/**
	 * add a entry to the bag. performs a compound save first to keep all other changes from the UI.
	 * Does nothing if not in EDIT mode.
	 * @param key
	 * @param value
	 */
	public void addBagProperty(String value) {
		if (componentMode != ICompound.MODE_EDIT)
			return;
		compound.save();
		internalModel.getMap().put(KEY_PREFIX + nextIndex++,  value);
	}

	/**
	 * save the components databag to the provided map. The model map will be cleared and than filled with the bags
	 * content. Elements with equal key will be lost.
	 */
	public void save() {
		compound.save();
		bag.save();
		model.clear();
		for (Entry<String, String> entry : internalModel.getMap().entrySet()) {
			String value = entry.getValue();
			model.add(value);
		}
		// load the model to the bag again if some entries are gone because of equal keys.
		load();
	}

	/**
	 * load the provided mode to the bag.
	 */
	public void load() {
		internalModel.getMap().clear();
		for (String value : model) {
			internalModel.getMap().put(KEY_PREFIX + nextIndex++, value);
		}
	}

	/**
	 * Set the data model
	 * 
	 * @param model
	 */
	public void setModel(Collection<String> model) {
		this.model = model;
	}

	/**
	 * @return retrieve the model
	 */
	public Collection<String> getModel() {
		return model;
	}

	/**
	 * Call this from your listener in preSave().
	 */
	public void preSaveAction() {
		save();
	}
	
	/**
	 * Call this from your listener in postSave(). Updates the model with the current state of the DataBag.
	 */
	public void postSaveAction() {
		componentMode = ICompound.MODE_READONLY;
		if (internalModel.getMap().size() > 0) {
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
		internalModel.getMap().clear();
		load();
		show();
	}

	/**
	 * Call this from your listener in preEdit(). The action controls to add and delete entries are made visible and the
	 * internal compound will be set to MODE_EDIT.
	 */
	public void preEditAction() {
		if (internalModel.getMap().size() > 0) {
			for (IButton button : removeButtons) {
				button.setVisible(true);
			}
			if (addButton != null)
				addButton.setVisible(true);
		}
		componentMode = ICompound.MODE_EDIT;
	}

	/**
	 * @return the components current mode.
	 */
	public int getComponentMode() {
		return componentMode;
	}

	/**
	 * Set the component mode to one of ICompound.MODE_EDIT or MODE_READONLY.
	 * 
	 * @param componentMode
	 */
	public void setComponentMode(int componentMode) {
		this.componentMode = componentMode;
	}

	
	public class InternalModel implements Serializable {

		private static final long serialVersionUID = 653877076979407476L;
		Map<String, String> map = new HashMap<String, String>();

		public Map<String, String> getMap() {
			return map;
		}

		public void setMap(Map<String, String> map) {
			this.map = map;
		}

	}	

}

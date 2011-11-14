package org.webguitoolkit.ui.addons;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.webguitoolkit.ui.base.IDataBag;
import org.webguitoolkit.ui.base.WebGuiFactory;
import org.webguitoolkit.ui.controls.AbstractPopup;
import org.webguitoolkit.ui.controls.BaseControl;
import org.webguitoolkit.ui.controls.Page;
import org.webguitoolkit.ui.controls.container.Canvas;
import org.webguitoolkit.ui.controls.container.ICanvas;
import org.webguitoolkit.ui.controls.container.IHtmlElement;
import org.webguitoolkit.ui.controls.event.ClientEvent;
import org.webguitoolkit.ui.controls.event.IActionListener;
import org.webguitoolkit.ui.controls.form.ButtonBar;
import org.webguitoolkit.ui.controls.form.Compound;
import org.webguitoolkit.ui.controls.form.IButton;
import org.webguitoolkit.ui.controls.form.IButtonBar;
import org.webguitoolkit.ui.controls.form.IButtonBarListener;
import org.webguitoolkit.ui.controls.form.ICompound;
import org.webguitoolkit.ui.controls.form.ICompoundLifecycleElement;
import org.webguitoolkit.ui.controls.form.ILabel;
import org.webguitoolkit.ui.controls.layout.SequentialTableLayout;
import org.webguitoolkit.ui.controls.tab.ITab;
import org.webguitoolkit.ui.controls.tab.ITabListener;
import org.webguitoolkit.ui.controls.tab.ITabStrip;
import org.webguitoolkit.ui.controls.tab.StandardTabStrip;
import org.webguitoolkit.ui.controls.table.AbstractTableListener;
import org.webguitoolkit.ui.controls.table.ITable;
import org.webguitoolkit.ui.controls.table.ITableListener;
import org.webguitoolkit.ui.controls.table.Table;
import org.webguitoolkit.ui.controls.util.Window2ActionAdapter;

/**
 * The Master-Detail-Controller coordinates the processing of a typical application pattern. Assuming to have a Table
 * (list) of objects to show which have connected detail information which might be distributed on one one many tabs in a
 * TabStrip.
 * 
 * Whenever a element in the master table is selected the details shall be updated.
 * 
 * @author peter@17sprints.de (derived from martin's factory)
 * 
 */
public class MasterDetailController implements Serializable {

	private static final long serialVersionUID = 1L;
	private Table table;
	private StandardTabStrip tabStrip;
	private ButtonBar masterButtonBar;

	private DelegateTableListener delegateTableListener;
	private DelegateTabListener delegateTabListener;

	private IMasterDetailViewListener viewListener;

	public MasterDetailController() {
	}

	/**
	 * Create a Master-Detail-Controller handling the row changes and tab changes including changes in EDIT mode.
	 * 
	 * @param table
	 *            the master table. The table listener has to be registered when calling this constructor. If a table
	 *            listener is present wrap it into the delegate table listener.
	 * @param tabStrip
	 *            the detail TabStrip. The TabStrip listener has to be registered when calling this constructor. If a
	 *            tab listener is present wrap it into the delegate tab listener.
	 * @param masterButtonBar
	 *            the master button bar usually from the first TabStrip.
	 * @param compounds
	 *            a list of all compounds depending on the master table. Usually all compounds on the tabstrip's tabs
	 */
	public MasterDetailController(ITable table, ITabStrip tabStrip, IButtonBar masterButtonBar,
			List<ICompound> compounds) {
		registerTable(table);
		registerTabStrip(tabStrip);
		registerCompounds(compounds);
		registerMasterButtonBar(masterButtonBar, true);
	}

	public IMasterDetailViewListener getViewListener() {
		return viewListener;
	}

	public void setViewListener(IMasterDetailViewListener viewListener) {
		this.viewListener = viewListener;
	}


	/**
	 * Register the table. If a table listener is present wrap it into the delegate table listener.
	 * 
	 * @param table
	 */
	private void registerTable(ITable table) {
		if (table == null)
			throw new IllegalArgumentException("Table must be not null");
		this.table = (Table) table;
		ITableListener tableListener = this.table.getListener();
		delegateTableListener = new DelegateTableListener();
		table.setListener(delegateTableListener);
		if (tableListener != null)
			delegateTableListener.setDelegate(tableListener);
	}

	/**
	 * register the tabstrip. If a tab listener is present wrap it into the delegate tab listener.
	 * 
	 * @param tabStrip
	 */
	private void registerTabStrip(ITabStrip tabStrip) {
		if (tabStrip == null)
			throw new IllegalArgumentException("Tabstrip must be not null");
		this.tabStrip = (StandardTabStrip) tabStrip;

		ITabListener tabListener = this.tabStrip.getListener();
		delegateTabListener = new DelegateTabListener();
		tabStrip.setListener(delegateTabListener);
		if (tabListener != null)
			delegateTabListener.setDelegate(tabListener);
	}

	/**
	 * Register the passed compound list at the TableListener and TabListener. Additionally the compounds are to grouped
	 * by Tabs.
	 * 
	 * @param compounds
	 *            complete list of compounds in the view
	 */
	private void registerCompounds(List<ICompound> compounds) {
		for (ICompound compound : compounds) {
			if (tabStrip == null)
				throw new IllegalStateException("TabStrip must be set before calling this");
			BaseControl parent = ((Compound) compound).getParent();
			while (parent.getParent() != null && parent.getParent() != tabStrip) {
				parent = parent.getParent();
			}
			if (parent.getParent() == tabStrip && parent instanceof ITab) {
				// register compound on the tab strip
				delegateTabListener.addCompound((ITab) parent, compound);
			}
		}
		delegateTableListener.setCompounds(compounds); // PZ new
	}

	private void registerMasterButtonBar(IButtonBar buttonBar, boolean callTableListenerOnNew) {
		if (buttonBar == null)
			throw new IllegalArgumentException("buttonBar must be not null");
		if (table == null)
			throw new IllegalStateException("table must be set before calling this");
		if (masterButtonBar != null)
			throw new IllegalStateException("masterButtonBar already set");
		masterButtonBar = (ButtonBar) buttonBar;

		IButtonBarListener listener = masterButtonBar.getListener();
		DelegateButtonBarListener delegateListener = new DelegateButtonBarListener(table, callTableListenerOnNew);
		masterButtonBar.setListener(delegateListener);
		if (listener != null)
			delegateListener.setDelegate(listener);
	}

	/**
	 * DelegateTableListener handles interaction with table events. In most cases just calls the delegate table
	 * listener.
	 */
	private class DelegateTableListener extends AbstractTableListener implements ITableListener {

		private static final long serialVersionUID = 1L;
		private ITableListener delegate = null;
		private List<ICompound> compounds = new ArrayList<ICompound>();

		public void setDelegate(ITableListener listener) {
			delegate = listener;
		}

		private ITableListener getDelegate() {
			if (delegate == null)
				return new AbstractTableListener() {
					@Override
					public void onRowSelection(ITable table, int row) {
					}
					private static final long serialVersionUID = 1L;
				};
			return delegate;
		}

		public void setCompounds(List<ICompound> compounds) {
			this.compounds = compounds;
		}

		@Override
		public void onGotoRow(ClientEvent event) {
			Table table = (Table) event.getSource();
			// if there is a least one row...
			boolean success = true;
			if (table.getRowsLoaded() > 0) {
				int userRow = table.getPage().getContext().getValueAsInt(table.id4RowInput()) - 1;
				// set user row into bounds
				userRow = Math.max(0,
						Math.min(userRow, table.getPage().getContext().getValueAsInt(table.id4size()) - 1));
				IDataBag bag = table.getRow(userRow);
				success = handleRowChange(bag, event, false);
			}
			if (success)
				getDelegate().onGotoRow(event);
		}

		@Override
		public void onRowSelected(ClientEvent event) {
			// Reference to the Table is always good
			Table table = (Table) event.getSource();
			// we do the controlling of the highlight
			int rowSelected = Integer.parseInt(event.getParameter(0));
			// there are some misguided events, when the table size is smaller than
			// the viewable size
			int firstRow = table.getPage().getContext().getValueAsInt(table.id4FirstRow());
			int absSelection = firstRow + rowSelected;
			IDataBag bag = table.getRow(absSelection);
			boolean success = handleRowChange(bag, event, true);
			if (success)
				getDelegate().onRowSelected(event);
		}

		@Override
		public void onRowSelection(ITable table, int row) {
			for (ICompound compound : compounds) {
				compound.setBag(table.getRow(row));
				compound.load();
			}
			if (getDelegate() instanceof AbstractTableListener)
				((AbstractTableListener) getDelegate()).onRowSelection(table, row);
		}

		/**
		 * Handle row change including EDIT mode check.<br>
		 * 1. Collect all compounds in EDIT mode.<br>
		 * 1a. If any in EDIT mode show "Unsaved Changes" dialog <br>
		 * 1b. if non load all compounds
		 * 
		 * @return <code>false</code> if some compound are in edit mode
		 */
		private boolean handleRowChange(IDataBag newItem, ClientEvent evnet, boolean isRowSelect) {
			List<ICompound> compoundsInEditMode = new ArrayList<ICompound>();

			// look for compounds in EDIT mode
			for (ICompound compound : compounds) {
				if (compound.getMode() == ICompound.MODE_EDIT || compound.getMode() == ICompound.MODE_NEW) {
					compoundsInEditMode.add(compound);
				}
			}

			// if compounds in EDIT mode exist raise dialog
			if (!compoundsInEditMode.isEmpty()) {
				new UnsavedCompoundsView(WebGuiFactory.getInstance(), table.getPage(), new RowChangeConfirmListener(delegate,
						evnet, compounds, compoundsInEditMode, isRowSelect, newItem,
						((Table) table).getSelectedRowIndex())).show();
				return false;
			}

			for (ICompound compound : compounds) {
				compound.setBag(newItem);
				compound.load();
			}
			
			if (viewListener != null)
				viewListener.onRowChange(newItem);
			
			return true;
		}

		@Override
		public void onCheckAll(ClientEvent event) {
			getDelegate().onCheckAll(event);
		}

		@Override
		public void onClickDown(ClientEvent event) {
			getDelegate().onClickDown(event);
		}

		@Override
		public void onClickUp(ClientEvent event) {
			getDelegate().onClickUp(event);
		}

		@Override
		public void onDDFilter(ClientEvent event) {
			getDelegate().onDDFilter(event);
		}

		@Override
		public void onDragStart(ClientEvent event, String draggableId) {
			getDelegate().onDragStart(event, draggableId);
		}

		@Override
		public void onDropped(ClientEvent event, String draggabelId, String droppableId) {
			getDelegate().onDropped(event, draggabelId, droppableId);
		}

		@Override
		public void onEditTableLayout(ClientEvent event, int rowCount, String tableSetting) {
			getDelegate().onEditTableLayout(event, rowCount, tableSetting);
		}

		@Override
		public void onGotoBottom(ClientEvent event) {
			getDelegate().onGotoBottom(event);
		}

		@Override
		public void onScrollTo(ClientEvent event) {
			getDelegate().onScrollTo(event);
		}

		@Override
		public void onSort(ClientEvent event) {
			getDelegate().onSort(event);
		}

		@Override
		public void onGotoTop(ClientEvent event) {
			getDelegate().onGotoTop(event);
		}

		@Override
		public void onImplicitFilter(ClientEvent event) {
			getDelegate().onImplicitFilter(event);
		}

		@Override
		public void onPageDown(ClientEvent event) {
			getDelegate().onPageDown(event);
		}

		@Override
		public void onPageUp(ClientEvent event) {
			getDelegate().onPageUp(event);
		}

		public class RowChangeConfirmListener implements IConfirmationListener {
			private final boolean isRowSelelct;
			private final List<ICompound> allCompounds;
			private final List<ICompound> compoundsInEdit;
			private final ClientEvent tableEvent;
			private final ITableListener delegatedListener;
			private final IDataBag newItem;
			private final int oldSelectionId;

			public RowChangeConfirmListener(ITableListener delegate, ClientEvent tableEvent, List<ICompound> allCompounds,
					List<ICompound> compoundsInEdit, boolean isRowSelelct, IDataBag newItem, int oldSelection) {
				super();
				this.isRowSelelct = isRowSelelct;
				this.allCompounds = allCompounds;
				this.compoundsInEdit = compoundsInEdit;
				this.tableEvent = tableEvent;
				this.delegatedListener = delegate;
				this.newItem = newItem;
				this.oldSelectionId = oldSelection;
			}

			public void onYes(ClientEvent event) {
				for (ICompound compound : compoundsInEdit) {
					compound.getBag().undo();
					compound.changeElementMode(ICompound.MODE_READONLY);
				}
				
				for (ICompound compound : allCompounds) {
					compound.setBag(newItem);
					compound.load();
				}
				if (delegate != null) {
					if (isRowSelelct)
						delegatedListener.onRowSelected(tableEvent);
					else
						delegatedListener.onGotoRow(tableEvent);
				}
			}

			public void onNo(ClientEvent event) {
				// selectionChange has to be called twice cause
				// the old selection may be the same in the context
				if (masterButtonBar != null && compoundsInEdit.contains(masterButtonBar.surroundingCompound())
						&& masterButtonBar.surroundingCompound().getMode() == ICompound.MODE_NEW) {
					((Table) tableEvent.getSource()).selectionChange(0, false);
					((Table) tableEvent.getSource()).selectionChange(-1, false);
				} else {
					((Table) tableEvent.getSource()).selectionChange(-1, false);
					((Table) tableEvent.getSource()).selectionChange(oldSelectionId, false);
				}
			}
		}
	}

	/**
	 * DelegateTableListener handles interaction with tab events
	 */
	public class DelegateTabListener implements ITabListener {
	
		private static final long serialVersionUID = 1L;
		private ITabListener delegate = null;
		private final Hashtable<ITab,List<ICompound>> compoundsOnTab = new Hashtable<ITab, List<ICompound>>();

		public void setDelegate(ITabListener listener) {
			delegate = listener;
		}

		public void addCompound(ITab tab, ICompound compound) {
			List<ICompound> comps = compoundsOnTab.get(tab);
			if (comps == null) {
				comps = new ArrayList<ICompound>();
				compoundsOnTab.put(tab, comps);
			}
			comps.add(compound);
		}

		public boolean onTabChange(ITab old, ITab selected, ClientEvent event) {
			List<ICompound> compounds = compoundsOnTab.get(old);
			List<ICompound> compoundsInEditMode = new ArrayList<ICompound>();
			if (compounds != null) {
				for (ICompound compound :compounds) {
					if (compound.getMode() == ICompound.MODE_EDIT || compound.getMode() == ICompound.MODE_NEW) {
						compoundsInEditMode.add(compound);
					}
				}
			}
			if (!compoundsInEditMode.isEmpty()) {
				new UnsavedCompoundsView(WebGuiFactory.getInstance(), table.getPage(), new TabChangeConfirmListener(delegate,
						event, compoundsInEditMode, old, selected)).show();
				return false;
			}
			if (delegate != null)
				return delegate.onTabChange(old, selected, event);
			return true;
		}

		public class TabChangeConfirmListener implements IConfirmationListener {
			private final ITab oldTab;
			private final ITab newTab;
			private final List<ICompound> compounds;
			private final ClientEvent tabEvent;
			private final ITabListener delegateTabListener;

			public TabChangeConfirmListener(ITabListener delegate, ClientEvent tabEvent, List<ICompound> compounds, ITab oldTab,
					ITab newTab) {
				super();
				this.oldTab = oldTab;
				this.newTab = newTab;
				this.compounds = compounds;
				this.tabEvent = tabEvent;
				this.delegateTabListener = delegate;
			}

			public void onYes(ClientEvent event) {
				if (masterButtonBar != null && compounds.contains(masterButtonBar.surroundingCompound())
						&& masterButtonBar.surroundingCompound().getMode() == ICompound.MODE_NEW) {
					DelegateButtonBarListener listener = (DelegateButtonBarListener) ((ButtonBar) masterButtonBar)
							.getListener();
					int oldTableSelection = listener.getOldTableSelection();
					((Table) table).selectionChange(oldTableSelection, true);
				}
				for (ICompound comp : compounds) {
					comp.getBag().undo();
					comp.load();
					comp.changeElementMode(ICompound.MODE_READONLY);
				}
				if (delegateTabListener != null)
					delegateTabListener.onTabChange(oldTab, newTab, tabEvent);
				else
					oldTab.getTabStrip().selectTab(newTab);
			}

			public void onNo(ClientEvent event) {
				// nothing to do
			}
		}

	}

	/**
	 * DelegateTableListener handles interaction with button bar events and calls the
	 */
	public class DelegateButtonBarListener implements IButtonBarListener {
		private static final long serialVersionUID = 1L;

		private final ITable masterTable;

		private IButtonBarListener delegate;

		private int oldTableSelection = -1;

		private final boolean callTableListenerOnNew;

		public DelegateButtonBarListener(ITable table, boolean callTableListenerOnNew) {
			this.masterTable = table;
			this.callTableListenerOnNew = callTableListenerOnNew;
		}

		public void setDelegate(IButtonBarListener listener) {
			this.delegate = listener;
		}

		public void onCancel(ClientEvent event) {
			if (getCompound(event).getMode() == ICompound.MODE_NEW && oldTableSelection != -1)
				((Table) masterTable).selectionChange(oldTableSelection, true);
			if (delegate != null)
				delegate.onCancel(event);
		}

		public void onDelete(ClientEvent event) {
			if (delegate != null)
				delegate.onDelete(event);
			if (getCompound(event).hasErrors())
				return;
			((Table) masterTable).removeAndReload(getCompound(event).getBag());
			if (masterTable.getDefaultModel().getTableData().size() > 0) {
				masterTable.selectionChange(0, true);
			} else {
				masterTable.selectionChange(-1, false);
				getCompound(event).setBag(null);
				getCompound(event).load();
			}
		}

		public void onEdit(ClientEvent event) {
			if (delegate != null)
				delegate.onEdit(event);
		}

		public void onNew(ClientEvent event) {
			if (delegate != null)
				delegate.onNew(event);
			oldTableSelection = ((Table) masterTable).getSelectedRowIndex();
			masterTable.selectionChange(-1, false);
		}

		public void onSave(ClientEvent event) {
			boolean isNew = getCompound(event).getMode() == ICompound.MODE_NEW;
			if (delegate != null)
				delegate.onSave(event);
			if (isNew && !getCompound(event).hasErrors()) {
				if (callTableListenerOnNew)
					((Table) masterTable).selectionChange(0, true);

			} else if (!getCompound(event).hasErrors())
				masterTable.load();
		}

		private ICompound getCompound(ClientEvent event) {
			return ((ICompoundLifecycleElement) event.getSource()).surroundingCompound();
		}

		public int getOldTableSelection() {
			return oldTableSelection;
		}

	}

	/**
	 * Confirmation dialog that is displayed when there are compounds in edit mode
	 */
	class UnsavedCompoundsView extends AbstractPopup {
		private static final long serialVersionUID = 1L;
		private final IConfirmationListener proceedListener;

		public UnsavedCompoundsView(WebGuiFactory factory, Page page, IConfirmationListener proceedListener) {
			super(factory, page, "popup.header.unsaved.compounds@There are unsaved Forms!", 250, 400);
			this.proceedListener = proceedListener;
		}

		@Override
		protected void createControls(WebGuiFactory factory, ICanvas viewConnector) {
			String msgType = "warn";
			String msg = "popup.body.unsaved.compounds@There are unsaved Forms! <br/>Proceed and discard changes?";

			// the canvas will be inserted manually into the page
			viewConnector.setDragable(true);
			viewConnector.setDisplayMode(Canvas.DISPLAY_MODE_WINDOW_MODAL);
			viewConnector.addCssClass("wgtPopup wgtAlertBox");

			if (StringUtils.isNotEmpty(msgType))
				viewConnector.setTitle("msgbox." + msgType + "@" + msgType.substring(0, 1).toUpperCase()
						+ msgType.substring(1));

			IActionListener noListener = new IActionListener() {
				public void onAction(ClientEvent event) {
					proceedListener.onNo(event);
					close();
				}
				private static final long serialVersionUID = 1L;
			};

			IActionListener yesListener = new IActionListener() {
				public void onAction(ClientEvent event) {
					proceedListener.onYes(event);
					close();
				}
				private static final long serialVersionUID = 1L;
			};

			viewConnector.setWindowActionListener(new Window2ActionAdapter(noListener));

			SequentialTableLayout manager = new SequentialTableLayout();
			manager.setTableStyle("width: 100%;");
			viewConnector.setLayout(manager);

			// add image
			if (StringUtils.isNotEmpty(msgType)) {
				IHtmlElement img = factory.createHtmlElement(viewConnector, "img");
				img.setAttribute("src", "./images/wgt/icons/msg_icon_" + msgType + ".gif");
				img.setLayoutData(SequentialTableLayout.getLayoutData(0, 0, false).setCellStyle(
						"text-align: center; width: 40px;"));
			}

			// add label
			ILabel infoLabel = factory.createLabel(viewConnector, msg);
			infoLabel.setLayoutData(SequentialTableLayout.getLastInRow().setCellStyle("text-align: left;"));

			// add button yes
			IButton yesButton = factory.createButton(viewConnector, null, "button.yes@Yes", "button.yes@Yes",
					yesListener);
			if (StringUtils.isNotEmpty(msgType))
				yesButton.setLayoutData(SequentialTableLayout.getLastInRow().setCellColSpan(2)
						.setCellStyle("text-align: center;"));
			else
				yesButton.setLayoutData(SequentialTableLayout.getLastInRow().setCellStyle("text-align: center;"));

			// add button no
			IButton noButton = factory.createButton(viewConnector, null, "button.no@No", "button.no@No", noListener);
			noButton.setLayoutData(SequentialTableLayout.APPEND);
			getDialog().getWindow().addCssClass("dropshadow");
		}
	}

	/**
	 * listener that handles the confirmation result.
	 */
	interface IConfirmationListener {
		void onYes(ClientEvent event);

		void onNo(ClientEvent event);
	}

	public interface IMasterDetailViewListener {
		public void onRowChange(IDataBag bag);
	}

}

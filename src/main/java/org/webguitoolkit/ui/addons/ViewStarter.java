package org.webguitoolkit.ui.addons;

import org.webguitoolkit.ui.controls.AbstractView;
import org.webguitoolkit.ui.controls.event.ClientEvent;
import org.webguitoolkit.ui.controls.event.IActionListener;

/**
 * Simple class that can be used as ActionListener for Buttons etc. to start a view.
 * 
 * @author peter@17sprints.de
 * 
 */
public class ViewStarter implements IActionListener {

	private static final long serialVersionUID = -7701022300010531328L;

	private AbstractView viewToStart;

	public ViewStarter(AbstractView viewToStart) {
		this.viewToStart = viewToStart;
	}

	public void onAction(ClientEvent event) {
		viewToStart.show();
	}
}
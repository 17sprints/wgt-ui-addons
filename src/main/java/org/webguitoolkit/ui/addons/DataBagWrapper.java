package org.webguitoolkit.ui.addons;

import java.util.Map;
import java.util.Map.Entry;

import org.webguitoolkit.ui.base.DataBag;
import org.webguitoolkit.ui.base.IDataBag;

/**
 * Provides access to the property map of DataBag.
 * 
 * @author peter@17sprints.de
 * 
 */

public class DataBagWrapper extends DataBag implements IDataBag {

	private static final long serialVersionUID = -3912569684924615130L;

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
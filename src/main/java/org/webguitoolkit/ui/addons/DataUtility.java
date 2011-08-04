package org.webguitoolkit.ui.addons;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.webguitoolkit.ui.base.DataBag;
import org.webguitoolkit.ui.base.IDataBag;

/**
 * Little helpers
 * 
 * @author peter@17sprints.de
 * 
 */
public class DataUtility {

	/**
	 * Wrap the elements of the passed collection with DataBags
	 * 
	 * @param collection
	 *            the items to wrap
	 * @return a List with DataBags containing the elements from the passed collection
	 */
	public static List<IDataBag> wrap(Collection collection) {
		List<IDataBag> result = new ArrayList<IDataBag>();
		for (Object o : collection) {
			result.add(new DataBag(o));
		}
		return result;
	}
}

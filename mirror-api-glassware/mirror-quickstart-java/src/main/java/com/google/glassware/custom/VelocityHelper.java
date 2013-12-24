package com.google.glassware.custom;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

public class VelocityHelper {

	private static final Logger LOG = Logger.getLogger(VelocityHelper.class.getSimpleName());

	public static void initVelocity() {
		// Globally init Velocity
		try {
			Properties p = new Properties();
			p.put("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
			p.put(RuntimeConstants.RESOURCE_LOADER, "classpath");
			Velocity.init(p);
		} catch (Exception e) {
			LOG.severe("Fail in init Velocity");
			e.printStackTrace();
		}

	}

	public static String getFinalStr(Map data, String tempalteName) {
		Iterator iter = data.keySet().iterator();
		VelocityContext context = new VelocityContext();

		while (iter.hasNext()) {
			String key = (String) iter.next();
			context.put(key, data.get(key));
		}
		String result = "";
		try {
			Template template = Velocity.getTemplate("com/google/glassware/custom/vm/" + tempalteName);
			StringWriter sw = new StringWriter();
			template.merge(context, sw);
			result = sw.toString();
		} catch (Exception e) {
			LOG.severe("Can not process data for template:" + tempalteName + " data:" + data);
			e.printStackTrace();
		}

		return result;

	}

	public static void main(String[] args) {
		VelocityHelper.initVelocity();
		VelocityContext context = new VelocityContext();

		context.put("quantity", "1234");

		Template template = null;

		try {
			template = Velocity.getTemplate("com/google/glassware/custom/vm/itemConver.vm");
			StringWriter sw = new StringWriter();
			template.merge(context, sw);
			System.out.println(sw.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
		Map data = new HashMap();
		List list = new ArrayList();
		list.add("note1");
		list.add("note2");
		data.put(Constants.ITEM_COL_IMGURL, "imgurl_str");
		data.put(Constants.ITEM_COL_NOTES, list);
		data.put(Constants.ITEM_COL_PRDNAME, "prdname_str");
		data.put(Constants.ITEM_COL_PRDNUM, 1);
		data.put(Constants.ITEM_COL_PRICE, 999999.99);
		data.put(Constants.ITEM_COL_PROMO, "promo str");
		data.put(Constants.ITEM_COL_PURCHASED,true);
		data.put(Constants.ITEM_COL_QUANTITY, 2);
		
		
		System.out.println(VelocityHelper.getFinalStr(data, "itemConver.vm"));
	}

}

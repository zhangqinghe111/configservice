package com.weibo.dorteam.Common;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonCommon {

	public static Object getJavabean(String jsonString, Class classes)
			throws JsonParseException, JsonMappingException, IOException {
		if (jsonString == null || jsonString.equals("") || classes == null)
			return null;
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
				false);
		mapper.configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
		Object myObject = mapper.readValue(jsonString, classes);
		return myObject;
	}
}

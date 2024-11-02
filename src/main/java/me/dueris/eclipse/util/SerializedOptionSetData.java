package me.dueris.eclipse.util;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class SerializedOptionSetData implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	public List<String> detectedSpecs;
	public Map<String, String> detectedOptions;
	public Map<String, List<String>> optionsToArguments;
	public Map<String, String> recognizedSpecs;
	public Map<String, List<?>> defaultValues;
}
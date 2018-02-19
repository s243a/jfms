package jfms.config;

import java.util.List;

public class ChoiceValidator implements ConfigEntryValidator {
	private final List<String> allowedValues;

	public ChoiceValidator(List<String> allowedValues) {
		this.allowedValues = allowedValues;
	}

	@Override
	public boolean validate(String value) {
		return allowedValues.contains(value);
	}

	public List<String> getAllowedValues() {
		return allowedValues;
	}
}

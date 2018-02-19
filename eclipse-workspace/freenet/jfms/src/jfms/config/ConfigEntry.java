package jfms.config;

public class ConfigEntry {
	private final String propertyName;
	private final ConfigType type;
	private final String name;
	private final String defaultValue;
	private final ConfigEntryValidator validator;
	private final String description;

	public static boolean isBooleanValue(String value) {
		return "true".equals(value) || "false".equals(value);
	}

	public static boolean isIntValue(String value) {
		try {
			Integer.parseInt(value);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	public ConfigEntry(String propertyName, ConfigType type, String name,
			String defaultValue, ConfigEntryValidator validator,
			String description) {

		this.propertyName = propertyName;
		this.type = type;
		this.name = name;
		this.defaultValue = defaultValue;
		this.validator = validator;
		this.description = description;
	}

	public ConfigEntry(String propertyName, ConfigType type, String name,
			String defaultValue, String description) {

		this.propertyName = propertyName;
		this.type = type;
		this.name = name;
		this.validator = null;
		this.description = description;
		this.defaultValue = defaultValue;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public String getName() {
		return name;
	}

	public ConfigType getType() {
		return type;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public ConfigEntryValidator getValidator() {
		return validator;
	}

	public String getDescription() {
		return description;
	}

	public boolean validate(String value) {
		switch (type) {
		case BOOLEAN:
			if (!isBooleanValue(value)) {
				return false;
			}
			break;
		case INT:
			if (!isIntValue(value)) {
				return false;
			}
			break;
		case STRING:
			break;
		}

		if (validator == null) {
			return true;
		}

		return validator.validate(value);
	}
}

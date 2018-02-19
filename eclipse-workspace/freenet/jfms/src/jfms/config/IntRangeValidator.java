package jfms.config;

public class IntRangeValidator implements ConfigEntryValidator {
	private final Integer minValue;
	private final Integer maxValue;

	public IntRangeValidator(Integer minValue, Integer maxValue) {
		this.minValue = minValue;
		this.maxValue = maxValue;
	}

	@Override
	public boolean validate(String value) {
		if (value == null) {
			return false;
		}

		try {
			int intValue = Integer.parseInt(value);
			if (minValue != null && intValue < minValue) {
				return false;
			}

			if (maxValue != null && intValue > maxValue) {
				return false;
			}

			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
}

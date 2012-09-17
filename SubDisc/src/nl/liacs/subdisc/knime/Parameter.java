package nl.liacs.subdisc.knime;

import org.knime.core.node.defaultnodesettings.*;

/*
 * NOTE for all SettingsModels the 2nd parameter is the default value
 * some of these will be filled in based on the data
 * TODO for the others, defaults should come from Cortana
 */
public enum Parameter {
	PRIMARY_TARGET {
		@Override
		SettingsModel getSettingModel() {
			return new SettingsModelString(name(), null);
		}
	},
	QUALITY_MEASURE {
		@Override
		SettingsModel getSettingModel() {
			// WRAcc
			return new SettingsModelString(name(), CortanaSettings.QUALITY_MEASURES[0]);
		}
	},
	MEASURE_MINIMUM {
		@Override
		SettingsModel getSettingModel() {
			return new SettingsModelDouble(name(), 0.01);
		}
	},
	TARGET_VALUE {
		@Override
		SettingsModel getSettingModel() {
			return new SettingsModelString(name(), null);
		}
	},
	REFINEMENT_DEPTH {
		@Override
		SettingsModel getSettingModel() {
			return new SettingsModelInteger(name(), 1);
		}
	},
	MINIMUM_COVERAGE {
		@Override
		SettingsModel getSettingModel() {
			return new SettingsModelInteger(name(), 1);
		}
	},
	MAXIMUM_COVERAGE_FRACTION {
		// value should be 10% of number of rows in data set
		@Override
		SettingsModel getSettingModel() {
			return new SettingsModelDouble(name(), 1.0);
		}
	},
	MAXIMUM_SUBGROUPS {
		@Override
		SettingsModel getSettingModel() {
			return new SettingsModelInteger(name(), 50);
		}
	},
	MAXIMUM_TIME {
		@Override
		SettingsModel getSettingModel() {
			return new SettingsModelDouble(name(), 1.0);
		}
	},
	SEARCH_STRATEGY {
		@Override
		SettingsModel getSettingModel() {
			return new SettingsModelString(name(), "beam");
		}
	},
	SEACH_WIDTH {
		@Override
		SettingsModel getSettingModel() {
			return new SettingsModelInteger(name(), 100);
		}
	},
	SET_VALUED_NOMINALS {
		@Override
		SettingsModel getSettingModel() {
			return new SettingsModelBoolean(name(), false);
		}
	},
	NUMERIC_OPERATORS {
		@Override
		SettingsModel getSettingModel() {
			return new SettingsModelString(name(), "<html>&#8804;, &#8805;</html>");
		}
	},
	NUMERIC_STRATEGY {
		@Override
		SettingsModel getSettingModel() {
			return new SettingsModelString(name(), "bins");
		}
	},
	NUMBER_OF_BINS {
		@Override
		SettingsModel getSettingModel() {
			return new SettingsModelInteger(name(), 8);
		}
	},
	NUMBER_OF_THREADS {
		@Override
		SettingsModel getSettingModel() {
			return new SettingsModelInteger(name(), 0);
		}
	};

	@Override
	public String toString() {
		if (this == SET_VALUED_NOMINALS)
			return "set-valued nominals";
		return name().toLowerCase().replace("_", " ");
	}

	abstract SettingsModel getSettingModel();
}

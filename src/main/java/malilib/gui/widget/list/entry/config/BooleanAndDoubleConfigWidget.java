package malilib.gui.widget.list.entry.config;

import malilib.config.option.BooleanAndDoubleConfig;
import malilib.config.option.BooleanAndDoubleConfig.BooleanAndDouble;
import malilib.gui.BaseScreen;
import malilib.gui.config.ConfigWidgetContext;
import malilib.gui.widget.DoubleTextFieldWidget.DoubleValidator;
import malilib.gui.widget.list.entry.DataListEntryWidgetData;

public class BooleanAndDoubleConfigWidget extends BaseBooleanAndNumberConfigWidget<BooleanAndDouble, BooleanAndDoubleConfig>
{
    public BooleanAndDoubleConfigWidget(BooleanAndDoubleConfig config,
                                        DataListEntryWidgetData constructData,
                                        ConfigWidgetContext ctx)
    {
        super(config, constructData, ctx,
              BooleanAndDoubleConfig::setValueFromString, BooleanAndDoubleConfig::getStringValue);

        this.textField.setTextValidator(new DoubleValidator(config.getMinDoubleValue(),
                                                            config.getMaxDoubleValue()));
        this.textField.translateAndAddHoverString("malilibdev.hover.config.numeric.range_and_default",
                                                  config.getMinDoubleValue(),
                                                  config.getMaxDoubleValue(),
                                                  config.getDefaultValue().doubleValue);
        this.sliderWidget.translateAndAddHoverString("malilibdev.hover.config.numeric.range_and_default",
                                                     config.getMinDoubleValue(),
                                                     config.getMaxDoubleValue(),
                                                     config.getDefaultValue().doubleValue);
    }

    @Override
    protected boolean onValueAdjustButtonClick(int mouseButton)
    {
        double amount = mouseButton == 1 ? -0.1 : 0.1;
        if (BaseScreen.isShiftDown()) { amount *= 20.0; }
        if (BaseScreen.isAltDown()) { amount *= 40.0; }

        this.config.setDoubleValue(this.config.getDoubleValue() + amount);
        this.updateWidgetState();

        return true;
    }
}

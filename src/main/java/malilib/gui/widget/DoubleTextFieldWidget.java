package malilib.gui.widget;

import javax.annotation.Nullable;
import malilib.gui.widget.util.TextFieldValidator;
import malilib.util.StringUtils;

public class DoubleTextFieldWidget extends BaseTextFieldWidget
{
    public DoubleTextFieldWidget(int width, int height)
    {
        this(width, height, 0);
    }

    public DoubleTextFieldWidget(int width, int height, double value)
    {
        this(width, height, value, Double.MIN_VALUE, Double.MAX_VALUE);
    }

    public DoubleTextFieldWidget(int width, int height, double initialValue, double minValue, double maxValue)
    {
        this(width, height, String.valueOf(initialValue), minValue, maxValue);
    }

    public DoubleTextFieldWidget(int width, int height, String initialValue, double minValue, double maxValue)
    {
        super(width, height, initialValue);

        this.setTextValidator(new DoubleValidator(minValue, maxValue));
    }

    public static class DoubleValidator implements TextFieldValidator
    {
        protected final double maxValue;
        protected final double minValue;

        public DoubleValidator(double minValue, double maxValue)
        {
            this.minValue = minValue;
            this.maxValue = maxValue;
        }

        @Override
        public boolean isValidInput(String text)
        {
            try
            {
                double value = Double.parseDouble(text);
                return value >= this.minValue && value <= this.maxValue;
            }
            catch (Exception ignore) {}

            return false;
        }

        @Override
        @Nullable
        public String getErrorMessage(String text)
        {
            try
            {
                double value = Double.parseDouble(text);

                if (value < this.minValue)
                {
                    return StringUtils.translate("malilibdev.message.error.text_field.value_below_min", String.valueOf(value), String.valueOf(this.minValue));
                }
                else if (value > this.maxValue)
                {
                    return StringUtils.translate("malilibdev.message.error.text_field.value_above_max", String.valueOf(value), String.valueOf(this.maxValue));
                }
                else if (Double.isNaN(value))
                {
                    return StringUtils.translate("malilibdev.message.error.text_field.invalid_value_float", text);
                }
            }
            catch (Exception e)
            {
                return StringUtils.translate("malilibdev.message.error.text_field.invalid_value_float", text);
            }

            return null;
        }
    }
}
